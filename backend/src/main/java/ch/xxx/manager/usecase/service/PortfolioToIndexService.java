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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.SortedMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableSortedMap;

import ch.xxx.manager.domain.exception.ResourceNotFoundException;
import ch.xxx.manager.domain.model.dto.QuoteDto;
import ch.xxx.manager.domain.model.entity.Currency;
import ch.xxx.manager.domain.model.entity.DailyQuote;
import ch.xxx.manager.domain.model.entity.DailyQuoteRepository;
import ch.xxx.manager.domain.model.entity.PortfolioToSymbol;
import ch.xxx.manager.domain.model.entity.PortfolioToSymbolRepository;
import ch.xxx.manager.domain.model.entity.SymbolRepository;
import ch.xxx.manager.domain.model.entity.dto.DailyQuoteEntityDto;
import jakarta.transaction.Transactional;

@Service
@Transactional
public class PortfolioToIndexService {
	record QuoteDtoAndWeight(BigDecimal weight, QuoteDto quoteDto) {
	}

	record PtsChangePair(PortfolioToSymbol ptsChange, Optional<PortfolioToSymbol> ptsChangeOld) {
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(PortfolioToIndexService.class);
	private final PortfolioToSymbolRepository portfolioToSymbolRepository;
	private final DailyQuoteRepository dailyQuoteRepository;
	private final CurrencyService currencyService;

	public PortfolioToIndexService(PortfolioToSymbolRepository portfolioToSymbolRepository,
			SymbolRepository symbolRepository, DailyQuoteRepository dailyQuoteRepository,
			CurrencyService currencyService) {
		this.portfolioToSymbolRepository = portfolioToSymbolRepository;
		this.dailyQuoteRepository = dailyQuoteRepository;
		this.currencyService = currencyService;
	}

	public List<DailyQuoteEntityDto> calculateIndexComparison(Long portfolioId, ComparisonIndex comparisonIndex) {
		return this.calculateIndexComparison(portfolioId, comparisonIndex, null, null);
	}

	private List<DailyQuoteEntityDto> compareToIndex(List<PortfolioToSymbol> portfolioChanges,
			List<DailyQuote> dailyQuotes) {
		record PtsChangesByDay(LocalDate day, PortfolioToSymbol ptsChange, Optional<PortfolioToSymbol> ptsChangeOld,
				Optional<DailyQuote> dailyQuoteOld) {
		}
		final AtomicReference<BigDecimal> currentWeight = calcWeight(portfolioChanges, dailyQuotes);

		List<PortfolioToSymbol> myPortfolioChanges = portfolioChanges.stream()
				.filter(pts -> Optional.ofNullable(pts.getChangedAt()).isPresent()
						|| Optional.ofNullable(pts.getRemovedAt()).isPresent())
				.collect(Collectors.toList());
		Map<String, List<PortfolioToSymbol>> symbolToPtsMap = myPortfolioChanges.stream()
				.collect(Collectors.groupingBy(pts -> pts.getSymbol().getSymbol(),
						Collectors.flatMapping((PortfolioToSymbol pts) -> Stream.of(pts), Collectors.toList())));
		symbolToPtsMap.entrySet().stream().map(entry -> {
			entry.setValue(entry.getValue().stream()
					.sorted((a, b) -> Optional.ofNullable(a.getChangedAt()).orElse(a.getRemovedAt())
							.compareTo(Optional.ofNullable(b.getChangedAt()).orElse(b.getRemovedAt())))
					.collect(Collectors.toList()));
			return entry;
		}).collect(Collectors.toMap(Entry::getKey, Entry::getValue));
		Map<LocalDate, List<PtsChangePair>> portfolioChangesMap = myPortfolioChanges.stream()
				.map(pts -> new PtsChangesByDay(Optional.ofNullable(pts.getChangedAt()).orElse(pts.getRemovedAt()), pts,
						findPreviousPts(symbolToPtsMap, pts),
						dailyQuotes.stream()
								.filter(myDailyQuote -> myDailyQuote.getLocalDay()
										.isEqual(Optional.ofNullable(pts.getChangedAt()).orElse(pts.getRemovedAt())))
								.findFirst()))
				.collect(Collectors.groupingBy(PtsChangesByDay::day, Collectors.flatMapping(
						pts -> Stream.of(new PtsChangePair(pts.ptsChange, pts.ptsChangeOld)), Collectors.toList())));
		SortedMap<LocalDate, List<PtsChangePair>> sortedPortfolioChangesMap = ImmutableSortedMap
				.copyOf(portfolioChangesMap, (date1, date2) -> date1.compareTo(date2));
		return dailyQuotes.stream().filter(x -> currentWeight.get().compareTo(BigDecimal.ZERO) > 0)
				.map(myDailyQuote -> new DailyQuoteEntityDto(myDailyQuote,
						this.calcQuote(myDailyQuote.getLocalDay(), sortedPortfolioChangesMap, myDailyQuote,
								currentWeight)))
				.filter(myRecord -> 0 >= BigDecimal.ZERO.compareTo(myRecord.dto().getClose()))
				.collect(Collectors.toList());
	}

	private AtomicReference<BigDecimal> calcWeight(List<PortfolioToSymbol> portfolioChanges,
			List<DailyQuote> dailyQuotes) {
		PortfolioToSymbol portfolioPts = portfolioChanges.stream()
				.filter(pts -> pts.getSymbol().getSymbol().contains(ServiceUtils.PORTFOLIO_MARKER)).findFirst()
				.orElseThrow(() -> new ResourceNotFoundException("Portfolio Symbol not found."));
		List<DailyQuote> myDailyQuotesPortfolio = dailyQuotes.stream()
				.sorted(Comparator.comparing(DailyQuote::getLocalDay)).collect(Collectors.toList());
		final Optional<DailyQuote> firstDailyQuotePortfolioOpt = this.dailyQuoteRepository
				.findBySymbolAndDayBetween(portfolioPts.getSymbol().getSymbol(),
						myDailyQuotesPortfolio.get(0).getLocalDay(),
						myDailyQuotesPortfolio.get(myDailyQuotesPortfolio.size() - 1).getLocalDay())
				.stream().findFirst();
		final Optional<DailyQuote> indexQuoteOpt = dailyQuotes.stream().filter(
				myDailyQuotes -> myDailyQuotesPortfolio.get(0).getLocalDay().isEqual(myDailyQuotes.getLocalDay()))
				.findFirst();
		final Currency currencyChange = indexQuoteOpt
				.map(indexQuote -> this.currencyService.getCurrencyQuote(portfolioPts, indexQuote))
				.orElse(Optional.of(new Currency(null, null, null, null, null, null, BigDecimal.ONE))).get();
		final BigDecimal iqAdjClose = indexQuoteOpt.stream().map(DailyQuote::getAdjClose).findFirst()
				.orElse(BigDecimal.ONE);
		final AtomicReference<BigDecimal> currentWeight = new AtomicReference<>(firstDailyQuotePortfolioOpt
				.map(myDailyQuote -> myDailyQuote.getAdjClose()
						.divide(iqAdjClose.multiply(currencyChange.getClose()), 10, RoundingMode.HALF_UP))
				.orElse(BigDecimal.ZERO));
		return currentWeight;
	}

	private Optional<PortfolioToSymbol> findPreviousPts(Map<String, List<PortfolioToSymbol>> symbolToPtsMap,
			PortfolioToSymbol pts) {
		List<PortfolioToSymbol> listCopy = new ArrayList<>(
				symbolToPtsMap.getOrDefault(pts.getSymbol().getSymbol(), List.of()));
		Collections.reverse(listCopy); // needs check
		return listCopy.stream().filter(myPts -> Optional.ofNullable(myPts.getChangedAt()).orElse(myPts.getRemovedAt())
				.isBefore(Optional.ofNullable(pts.getChangedAt()).orElse(pts.getRemovedAt()))).findFirst();
	}

	private QuoteDto calcQuote(LocalDate day, SortedMap<LocalDate, List<PtsChangePair>> portfolioChangesMap,
			DailyQuote dailyQuote, AtomicReference<BigDecimal> currentWeight) {
		if (dailyQuote.getLocalDay().isBefore(portfolioChangesMap.firstKey())) {
			throw new RuntimeException(String.format("No quotes for first portfolioChange: %s with Index: %s",
					portfolioChangesMap.firstKey().atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE),
					dailyQuote.getSymbol()));
		}
		return Optional.ofNullable(portfolioChangesMap.get(day)).isEmpty() ? new QuoteDto(
				dailyQuote.getOpen().multiply(currentWeight.get()), dailyQuote.getHigh().multiply(currentWeight.get()),
				dailyQuote.getLow().multiply(currentWeight.get()), dailyQuote.getClose().multiply(currentWeight.get()),
				dailyQuote.getAdjClose().multiply(currentWeight.get()), 0L, dailyQuote.getLocalDay().atStartOfDay(),
				dailyQuote.getSymbolKey(), dailyQuote.getSplit(), dailyQuote.getDividend())
				: portfolioChangesMap.get(day).stream().map(pts -> this.createQuote(day, pts, dailyQuote))
						.reduce(new QuoteDto(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
								BigDecimal.ZERO, -1L, day.atStartOfDay(), dailyQuote.getSymbol().getSymbol(), BigDecimal.ZERO, BigDecimal.ZERO),
								(acc, value) -> {
									acc.setOpen(acc.getOpen()
											.add(Optional.ofNullable(value.getOpen()).orElse(BigDecimal.ZERO)));
									acc.setHigh(acc.getHigh()
											.add(Optional.ofNullable(value.getHigh()).orElse(BigDecimal.ZERO)));
									acc.setLow(acc.getLow()
											.add(Optional.ofNullable(value.getLow()).orElse(BigDecimal.ZERO)));
									acc.setClose(acc.getClose()
											.add(Optional.ofNullable(value.getClose()).orElse(BigDecimal.ZERO)));
									acc.setAdjClose(acc.getAdjClose()
											.add(Optional.ofNullable(value.getAdjClose()).orElse(BigDecimal.ZERO)));
									acc.setVolume(0L);
									acc.setSplit(BigDecimal.ZERO);
									acc.setDividend(BigDecimal.ZERO);
									return acc;
								});
	}

	private QuoteDto createQuote(LocalDate day, PtsChangePair portfolioToSymbol, DailyQuote dailyQuote) {
		Currency currencyChange = this.currencyService.getCurrencyQuote(portfolioToSymbol.ptsChange(), dailyQuote)
				.orElse(new Currency(null, null, null, null, null, null, BigDecimal.ONE));
		Currency currencyChangeOld = this.currencyService.getCurrencyQuote(portfolioToSymbol.ptsChange(), dailyQuote)
				.orElse(new Currency(null, null, null, null, null, null, BigDecimal.ONE));
		BigDecimal changeOld = currencyChangeOld.getClose()
				.multiply(
						BigDecimal.valueOf(portfolioToSymbol.ptsChangeOld().map(PortfolioToSymbol::getWeight).orElse(0L)))
				.multiply(dailyQuote.getClose());
		BigDecimal change = currencyChange.getClose()
				.multiply(BigDecimal.valueOf(portfolioToSymbol.ptsChange().getWeight())).multiply(dailyQuote.getClose());
		BigDecimal changeAdjOld = currencyChangeOld.getClose()
				.multiply(
						BigDecimal.valueOf(portfolioToSymbol.ptsChangeOld().map(PortfolioToSymbol::getWeight).orElse(0L)))
				.multiply(dailyQuote.getAdjClose());
		BigDecimal changeAdj = currencyChange.getClose()
				.multiply(BigDecimal.valueOf(portfolioToSymbol.ptsChange().getWeight()))
				.multiply(dailyQuote.getAdjClose());
		return new QuoteDto(null, null, null, change.subtract(changeOld), changeAdj.subtract(changeAdjOld), 0L,
				day.atStartOfDay(), portfolioToSymbol.ptsChange().getSymbol().getSymbol(), null, null);
	}

	public List<DailyQuoteEntityDto> calculateIndexComparison(Long portfolioId, ComparisonIndex comparisonIndex,
			LocalDate from, LocalDate to) {
		List<PortfolioToSymbol> portfolioChanges = portfolioToSymbolRepository.findByPortfolioId(portfolioId);
		List<DailyQuote> dailyQuotes = Optional.ofNullable(from).filter(x -> Optional.ofNullable(to).isPresent())
				.map(x -> this.dailyQuoteRepository.findBySymbolAndDayBetween(comparisonIndex.getSymbol(), from, to))
				.orElse(this.dailyQuoteRepository.findBySymbol(comparisonIndex.getSymbol()));
		List<DailyQuoteEntityDto> result = compareToIndex(portfolioChanges, dailyQuotes);
		return result;
	}
}
