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
package ch.xxx.manager.usecase.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ch.xxx.manager.domain.exception.ResourceNotFoundException;
import ch.xxx.manager.domain.model.dto.PortfolioBarDto;
import ch.xxx.manager.domain.model.dto.PortfolioBarsDto;
import ch.xxx.manager.domain.model.dto.PortfolioDto;
import ch.xxx.manager.domain.model.entity.AppUserRepository;
import ch.xxx.manager.domain.model.entity.Portfolio;
import ch.xxx.manager.domain.model.entity.PortfolioRepository;
import ch.xxx.manager.domain.model.entity.PortfolioToSymbol;
import ch.xxx.manager.domain.model.entity.PortfolioToSymbolRepository;
import ch.xxx.manager.domain.model.entity.Symbol;
import ch.xxx.manager.domain.model.entity.Symbol.QuoteSource;
import ch.xxx.manager.domain.model.entity.SymbolRepository;
import ch.xxx.manager.domain.utils.CurrencyKey;
import ch.xxx.manager.usecase.service.PortfolioCalculationService.ComparisonIndexQuotes;

@Service
@Transactional
public class PortfolioService {
	private final PortfolioRepository portfolioRepository;
	private final PortfolioToSymbolRepository portfolioToSymbolRepository;
	private final SymbolRepository symbolRepository;
	private final AppUserRepository appUserRepository;
	private final PortfolioCalculationService portfolioCalculationService;
	private final PortfolioToIndexService portfolioToIndexService;

	public PortfolioService(PortfolioRepository portfolioRepository, AppUserRepository appUserRepository,
			PortfolioToSymbolRepository portfolioToSymbolRepository, SymbolRepository symbolRepository,
			PortfolioCalculationService portfolioCalculationService, PortfolioToIndexService portfolioToIndexService) {
		this.portfolioRepository = portfolioRepository;
		this.portfolioToSymbolRepository = portfolioToSymbolRepository;
		this.symbolRepository = symbolRepository;
		this.portfolioCalculationService = portfolioCalculationService;
		this.appUserRepository = appUserRepository;
		this.portfolioToIndexService = portfolioToIndexService;
	}

	public List<Portfolio> getPortfoliosByUserId(Long userId) {
		return this.portfolioRepository.findByUserId(userId);
	}

	public Portfolio getPortfolioById(Long portfolioId) {
		return this.portfolioRepository.findById(portfolioId)
				.orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + portfolioId));
	}

	public PortfolioBarsDto getPortfolioBarsByIdAndStart(Long portfolioId, LocalDate start) {
		Portfolio portfolio = this.portfolioRepository.findById(portfolioId)
				.orElseThrow(() -> new ResourceNotFoundException("Portfolio not found: " + portfolioId));
		List<PortfolioCalculationService.ComparisonIndexQuotes> comparisonQuotes = List.of(ComparisonIndex.values()).stream()
				.map(ci -> new PortfolioCalculationService.ComparisonIndexQuotes(ci,
						this.portfolioToIndexService.calculateIndexComparison(portfolioId, ci, start, LocalDate.now())))
				.toList();
		//this.portfolioCalculationService.calculatePortfolioBars(portfolio, start, comparisonQuotes);
		List<PortfolioBarDto> barDtos = List.of(
				new PortfolioBarDto(BigDecimal.valueOf(5.5D), "abc", BigDecimal.valueOf(20L)),
				new PortfolioBarDto(BigDecimal.valueOf(10.5D), "def", BigDecimal.valueOf(30L)),
				new PortfolioBarDto(BigDecimal.valueOf(15.5D), "hij", BigDecimal.valueOf(50L)));
		return new PortfolioBarsDto(portfolio.getName(), start, barDtos);
	}

	public Portfolio addPortfolio(Portfolio entity, Long userId) {
		entity.setAppUser(this.appUserRepository.findById(userId).orElse(null));
		Portfolio portfolio = this.portfolioRepository.save(entity);
		portfolio.getPortfolioToSymbols().add(createPortfolioPtSAndSymbol(portfolio));
		return portfolio;
	}

	public Portfolio addSymbolToPortfolio(PortfolioDto dto, Long symbolId, Long weight, LocalDateTime changedAt) {
		return this.portfolioCalculationService.calculatePortfolio(this.portfolioToSymbolRepository
				.save(this.createPtsEntity(dto, symbolId, weight, changedAt.toLocalDate())).getPortfolio());
	}

	public Portfolio updatePortfolioSymbolWeight(PortfolioDto dto, Long symbolId, Long weight,
			LocalDateTime changedAt) {
		return this.portfolioToSymbolRepository.findByPortfolioIdAndSymbolId(dto.getId(), symbolId).stream()
				.flatMap(myEntity -> Stream.of(
						this.updatePtsEntity(myEntity, Optional.of(weight), changedAt.toLocalDate(), Optional.empty())))
				.flatMap(newEntity -> Stream.of(this.portfolioToSymbolRepository.save(newEntity)))
				.map(newEntity -> this.portfolioCalculationService.calculatePortfolio(newEntity.getPortfolio()))
				.findFirst().orElseThrow(() -> new ResourceNotFoundException(
						String.format("Failed to remove symbol: %d from portfolio: %d", symbolId, dto.getId())));
	}

	public Portfolio removeSymbolFromPortfolio(Long portfolioId, Long symbolId, LocalDateTime removedAt) {
		return this.portfolioToSymbolRepository.findByPortfolioIdAndSymbolId(portfolioId, symbolId).stream()
				.flatMap(entity -> Stream.of(this.portfolioToSymbolRepository.save(this.updatePtsEntity(entity,
						Optional.empty(), LocalDate.now(), Optional.of(removedAt.toLocalDate())))))
				.map(newEntity -> this.portfolioCalculationService.calculatePortfolio(newEntity.getPortfolio()))
				.findFirst().orElseThrow(() -> new ResourceNotFoundException(
						String.format("Failed to remove symbol: %d from portfolio: %d", symbolId, portfolioId)));
	}

	private PortfolioToSymbol updatePtsEntity(PortfolioToSymbol entity, Optional<Long> weightOpt, LocalDate changedAt,
			Optional<LocalDate> removedAtOpt) {
		weightOpt.ifPresent(weight -> entity.setWeight(weight));
		removedAtOpt.ifPresentOrElse(date -> entity.setRemovedAt(date), () -> entity.setChangedAt(changedAt));
		return entity;
	}

	private PortfolioToSymbol createPtsEntity(PortfolioDto dto, Long symbolId, Long weight, LocalDate changedAt) {
		PortfolioToSymbol entity = new PortfolioToSymbol();
		entity.setPortfolio(this.portfolioRepository.findById(dto.getId()).map(myPts -> {
			myPts.getPortfolioToSymbols().add(entity);
			return myPts;
		}).orElse(null));
		entity.setSymbol(this.symbolRepository.findById(symbolId).map(myPts -> {
			myPts.getPortfolioToSymbols().add(entity);
			return myPts;
		}).orElse(null));
		entity.setWeight(weight);
		entity.setChangedAt(changedAt);
		return entity;
	}

	private PortfolioToSymbol createPortfolioPtSAndSymbol(Portfolio portfolioEntity) {
		Symbol symbolEntity = new Symbol(null, ServiceUtils.generateRandomPortfolioSymbol(), portfolioEntity.getName(),
				CurrencyKey.EUR, QuoteSource.PORTFOLIO, Set.of(), Set.of(), new HashSet<PortfolioToSymbol>());
		return this.portfolioToSymbolRepository
				.save(this.createPtSEntity(portfolioEntity, this.symbolRepository.save(symbolEntity)));
	}

	private PortfolioToSymbol createPtSEntity(Portfolio portfolioEntity, Symbol symbol) {
		PortfolioToSymbol ptSEntity = new PortfolioToSymbol();
		ptSEntity.setChangedAt(portfolioEntity.getCreatedAt());
		ptSEntity.setPortfolio(portfolioEntity);
		ptSEntity.setWeight(1L);
		ptSEntity.setSymbol(symbol);
		symbol.getPortfolioToSymbols().add(ptSEntity);
		return ptSEntity;
	}
}
