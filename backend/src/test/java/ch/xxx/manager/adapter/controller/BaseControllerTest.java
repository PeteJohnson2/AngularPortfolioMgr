package ch.xxx.manager.adapter.controller;

import javax.sql.DataSource;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;

import ch.xxx.manager.adapter.repository.JpaAppUserRepository;
import ch.xxx.manager.adapter.repository.JpaCurrencyRepository;
import ch.xxx.manager.adapter.repository.JpaDailyQuoteRepository;
import ch.xxx.manager.adapter.repository.JpaFinancialElementRepository;
import ch.xxx.manager.adapter.repository.JpaIntraDayQuoteRepository;
import ch.xxx.manager.adapter.repository.JpaPortfolioElementRepository;
import ch.xxx.manager.adapter.repository.JpaPortfolioRepository;
import ch.xxx.manager.adapter.repository.JpaPortfolioToSymbolRepository;
import ch.xxx.manager.adapter.repository.JpaRevokedTokenRepository;
import ch.xxx.manager.adapter.repository.JpaSectorRepository;
import ch.xxx.manager.adapter.repository.JpaSymbolFinancialsRepository;
import ch.xxx.manager.adapter.repository.JpaSymbolRepository;
import ch.xxx.manager.usecase.service.AppUserServiceDb;
import ch.xxx.manager.usecase.service.CurrencyService;
import ch.xxx.manager.usecase.service.FinancialDataService;
import ch.xxx.manager.usecase.service.PortfolioToIndexService;
import ch.xxx.manager.usecase.service.QuoteImportService;
import ch.xxx.manager.usecase.service.QuoteService;
import ch.xxx.manager.usecase.service.SymbolImportService;
import ch.xxx.manager.usecase.service.SymbolService;
import jakarta.persistence.EntityManager;

public class BaseControllerTest {
	@MockBean	
	protected AppUserServiceDb appUserServiceDb;
	@MockBean
	protected SymbolService symbolService;
	@MockBean 
	protected FinancialDataService financialDataService;
	@MockBean
	protected QuoteService quoteService;
	@MockBean
	protected QuoteImportService quoteImportService;
	@MockBean
	protected PortfolioToIndexService portfolioToIndexService;
	@MockBean
	protected CurrencyService currencyService;
	@MockBean
	protected SymbolImportService symbolImportService;
	@MockBean
	protected KafkaTemplate kafkaTemplate;
	@MockBean
	protected JpaAppUserRepository jpaAppUserRepository;
	@MockBean
	protected JpaCurrencyRepository jpaCurrencyRepository;
	@MockBean
	protected JpaDailyQuoteRepository jpaDailyQuoteRepository;
	@MockBean
	protected JpaFinancialElementRepository jpaFinancialElementRepository;
	@MockBean
	protected JpaIntraDayQuoteRepository jpaIntraDayQuoteRepository;
	@MockBean
	protected JpaPortfolioElementRepository jpaPortfolioElementRepository;
	@MockBean
	protected JpaPortfolioRepository jpaPortfolioRepository;
	@MockBean
	protected JpaPortfolioToSymbolRepository jpaPortfolioToSymbolRepository;
	@MockBean 
	protected JpaRevokedTokenRepository jpaRevokedTokenRepository;
	@MockBean
	protected JpaSectorRepository jpaSectorRepository;
	@MockBean
	protected JpaSymbolFinancialsRepository jpaSymbolFinancialsRepository;
	@MockBean
	protected EntityManager entityManager;
	@MockBean
	protected JpaSymbolRepository jpaSymbolRepository;
	@MockBean
	protected DataSource dataSource;
}
