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
package ch.xxx.manager.domain.model.entity;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;

import ch.xxx.manager.domain.utils.CurrencyKey;

@Entity
public class Symbol extends EntityBase {
	public enum QuoteSource {
		ALPHAVANTAGE, YAHOO, PORTFOLIO
	}

	private String symbol;
	private String name;
	@Enumerated(EnumType.STRING)
	private CurrencyKey currencyKey;
	@Enumerated(EnumType.STRING)
	private QuoteSource quoteSource;
	@OneToMany(mappedBy = "symbol", orphanRemoval = true)
	private Set<DailyQuote> dailyQuotes = new HashSet<>();
	@OneToMany(mappedBy = "symbol", orphanRemoval = true)
	private Set<IntraDayQuote> intraDayQuotes = new HashSet<>();
	@OneToMany(mappedBy = "symbol")
	private Set<PortfolioToSymbol> portfolioToSymbols = new HashSet<>();

	public Symbol() {
		super();
	}

	public Symbol(Long id, String symbol, String name, CurrencyKey currencyKey, QuoteSource quoteSource,
			Set<DailyQuote> dailyQuotes, Set<IntraDayQuote> intraDayQuotes, Set<PortfolioToSymbol> portfolioToSymbols) {
		super();
		super.setId(id);
		this.symbol = symbol;
		this.name = name;
		this.currencyKey = currencyKey;
		this.quoteSource = quoteSource;
		this.dailyQuotes = dailyQuotes;
		this.intraDayQuotes = intraDayQuotes;
		this.portfolioToSymbols = portfolioToSymbols;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public CurrencyKey getCurrencyKey() {
		return currencyKey;
	}

	public void setCurrencyKey(CurrencyKey currencyKey) {
		this.currencyKey = currencyKey;
	}

	public Set<DailyQuote> getDailyQuotes() {
		return dailyQuotes;
	}

	public void setDailyQuotes(Set<DailyQuote> dailyQuotes) {
		this.dailyQuotes = dailyQuotes;
	}

	public Set<IntraDayQuote> getIntraDayQuotes() {
		return intraDayQuotes;
	}

	public void setIntraDayQuotes(Set<IntraDayQuote> intraDayQuotes) {
		this.intraDayQuotes = intraDayQuotes;
	}

	public Set<PortfolioToSymbol> getPortfolioToSymbols() {
		return portfolioToSymbols;
	}

	public void setPortfolioToSymbols(Set<PortfolioToSymbol> portfolioToSymbols) {
		this.portfolioToSymbols = portfolioToSymbols;
	}

	public QuoteSource getQuoteSource() {
		return quoteSource;
	}

	public void setQuoteSource(QuoteSource quoteSource) {
		this.quoteSource = quoteSource;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getId());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Symbol other = (Symbol) obj;
		return Objects.equals(this.getId(), other.getId());
	}

	@Override
	public String toString() {
		return "Symbol [id=" + this.getId() + ", symbol=" + symbol + ", name=" + name + ", currencyKey=" + currencyKey
				+ ", quoteSource=" + quoteSource + ", dailyQuotes=" + dailyQuotes + ", intraDayQuotes=" + intraDayQuotes
				+ ", portfolioToSymbols=" + portfolioToSymbols + "]";
	}


}