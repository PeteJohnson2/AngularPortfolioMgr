/**
 *    Copyright 2019 Sven Loesekann
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
       http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package ch.xxx.manager.adapter.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import ch.xxx.manager.domain.model.dto.PortfolioBarsDto;
import ch.xxx.manager.domain.model.dto.PortfolioDto;
import ch.xxx.manager.domain.model.entity.dto.PortfolioWithElements;
import ch.xxx.manager.domain.utils.StreamHelpers;
import ch.xxx.manager.usecase.mapping.PortfolioMapper;
import ch.xxx.manager.usecase.service.ComparisonIndex;
import ch.xxx.manager.usecase.service.PortfolioService;

@RestController
@RequestMapping("rest/portfolio")
public class PortfolioController {
	private static final Logger LOGGER = LoggerFactory.getLogger(PortfolioController.class);

	private final PortfolioService portfolioService;
	private final PortfolioMapper portfolioMapper;

	public PortfolioController(PortfolioService portfolioService, PortfolioMapper portfolioMapper) {
		this.portfolioService = portfolioService;
		this.portfolioMapper = portfolioMapper;
	}

	@GetMapping("/userid/{userId}")
	public List<PortfolioDto> getPortfoliosByUserId(@PathVariable("userId") Long userId) {
		return this.portfolioService.getPortfoliosByUserId(userId).stream()
				.map(portWithElements -> this.portfolioMapper.toDto(portWithElements.portfolio(), portWithElements.portfolioElements()))
				.filter(StreamHelpers.distinctByKey(PortfolioDto::getId)).collect(Collectors.toList());
	}

	@GetMapping("/id/{portfolioId}")
	public PortfolioDto getPortfoliosById(@PathVariable("portfolioId") Long portfolioId,
			@RequestParam(name = "withHistory", required = false) Optional<String> withHistory) {
		PortfolioWithElements portfolioWithElements = this.portfolioService.getPortfolioById(portfolioId);
		return withHistory.stream().filter(myString -> "true".equalsIgnoreCase(myString))
				.map(xxx -> this.portfolioMapper.toDto(portfolioWithElements.portfolio(), portfolioWithElements.portfolioElements())).findFirst()
				.orElse(this.portfolioMapper.toDtoFiltered(portfolioWithElements.portfolio(), portfolioWithElements.portfolioElements()));
	}

	@GetMapping("/id/{portfolioId}/start/{start}")
	public PortfolioBarsDto getPortfolioBarsByIdAndStart(@PathVariable("portfolioId") Long portfolioId,
			@PathVariable("start") String isodateStart,
			@RequestParam(name = "compSymbols", required = false) List<String> compSymbols) {
		LocalDate start = LocalDate.parse(isodateStart, DateTimeFormatter.ISO_DATE);
		List<ComparisonIndex> compIndexes = StreamHelpers.toStream(compSymbols)
				.filter(cSym -> StreamHelpers.toStream(ComparisonIndex.values())
						.anyMatch(cIndex -> cIndex.getSymbol().equalsIgnoreCase(cSym)))
				.map(symStr -> StreamHelpers.toStream(ComparisonIndex.values())
						.filter(ci -> ci.getSymbol().equalsIgnoreCase(symStr)).findFirst())
				.flatMap(StreamHelpers::optionalStream).toList();
//		LOGGER.info(compIndexes.stream().map(value -> value.getSymbol()).collect(Collectors.joining(",")));
		return this.portfolioMapper
				.toBarsDto(this.portfolioService.getPortfolioBarsByIdAndStart(portfolioId, start, compIndexes));
	}
	
	@GetMapping("/countsymbols/userid/{userId}")
	public Long countPortfolioSymbolsByUserId(@PathVariable("userId") Long userId) {
		return this.portfolioService.countPortfolioSymbolsByUserId(userId);
	}

	@PostMapping
	public PortfolioDto createPortfolio(@RequestBody PortfolioDto dto) {
		return this.portfolioMapper
				.toDto(this.portfolioService.addPortfolio(this.portfolioMapper.toEntity(dto, null), dto.getUserId()), List.of());
	}

	@PostMapping("/symbol/{symbolId}/weight/{weight}")
	public PortfolioDto addSymbolToPortfolio(@RequestBody PortfolioDto dto, @PathVariable("symbolId") Long symbolId,
			@PathVariable("weight") Long weight, @RequestParam(name = "changedAt") String changedAt) {
		PortfolioWithElements portfolioWithElements = this.portfolioService
		.addSymbolToPortfolio(dto, symbolId, weight, this.isoDateTimeToLocalDateTime(changedAt));
		return this.portfolioMapper.toDtoFiltered(portfolioWithElements.portfolio(), portfolioWithElements.portfolioElements());
	}

	@PutMapping("/symbol/{symbolId}/weight/{weight}")
	public PortfolioDto updateSymbolToPortfolio(@RequestBody PortfolioDto dto, @PathVariable("symbolId") Long symbolId,
			@PathVariable("weight") Long weight, @RequestParam(name = "changedAt") String changedAt) {
		PortfolioWithElements portfolioWithElements = this.portfolioService
		.updatePortfolioSymbolWeight(dto.getId(), symbolId, weight, this.isoDateTimeToLocalDateTime(changedAt));
		return this.portfolioMapper.toDtoFiltered(portfolioWithElements
				.portfolio(), portfolioWithElements.portfolioElements());
	}

	@DeleteMapping("/{id}/symbol/{symbolId}")
	public PortfolioDto deleteSymbolFromPortfolio(@PathVariable("id") Long portfolioId,
			@PathVariable("symbolId") Long symbolId, @RequestParam(name = "removedAt") String removedAt) {
		PortfolioWithElements portfolioWithElements = this.portfolioService
		.removeSymbolFromPortfolio(portfolioId, symbolId, this.isoDateTimeToLocalDateTime(removedAt));
		return this.portfolioMapper.toDtoFiltered(portfolioWithElements.portfolio(), portfolioWithElements.portfolioElements());
	}

	private LocalDateTime isoDateTimeToLocalDateTime(String isoString) {
		if (isoString == null || isoString.trim().isBlank()) {
			return LocalDateTime.now();
		}
		String changedAtStr = UriUtils.decode(isoString, StandardCharsets.UTF_8.name());
		LOGGER.info(changedAtStr);
		return LocalDateTime.parse(changedAtStr, DateTimeFormatter.ISO_DATE_TIME);
	}
}
