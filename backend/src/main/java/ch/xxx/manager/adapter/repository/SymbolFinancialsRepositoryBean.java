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
package ch.xxx.manager.adapter.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import ch.xxx.manager.domain.model.dto.FilterNumberDto.Operation;
import ch.xxx.manager.domain.model.dto.FinancialElementParamDto;
import ch.xxx.manager.domain.model.dto.SfQuarterDto;
import ch.xxx.manager.domain.model.dto.SymbolFinancialsQueryParamsDto;
import ch.xxx.manager.domain.model.entity.FinancialElement;
import ch.xxx.manager.domain.model.entity.SymbolFinancials;
import ch.xxx.manager.domain.model.entity.SymbolFinancialsRepository;
import ch.xxx.manager.domain.utils.DataHelper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;

@Repository
public class SymbolFinancialsRepositoryBean implements SymbolFinancialsRepository {
	private static final Logger LOGGER = LoggerFactory.getLogger(SymbolFinancialsRepositoryBean.class);
	private final JpaSymbolFinancialsRepository jpaSymbolFinancialsRepository;
	private final EntityManager entityManager;

	public SymbolFinancialsRepositoryBean(JpaSymbolFinancialsRepository jpaSymbolFinancialsRepository,
			EntityManager entityManager) {
		this.jpaSymbolFinancialsRepository = jpaSymbolFinancialsRepository;
		this.entityManager = entityManager;
	}

	@Override
	public SymbolFinancials save(SymbolFinancials symbolfinancials) {
		return this.jpaSymbolFinancialsRepository.save(symbolfinancials);
	}

	@Override
	public List<SymbolFinancials> saveAll(Iterable<SymbolFinancials> symbolfinancials) {
		return this.jpaSymbolFinancialsRepository.saveAll(symbolfinancials);
	}

	@Override
	public Optional<SymbolFinancials> findById(Long id) {
		return this.jpaSymbolFinancialsRepository.findById(id);
	}

	@Override
	public void deleteAllBatch() {
		this.jpaSymbolFinancialsRepository.deleteAllInBatch();
	}

	public List<SfQuarterDto> findCommonSfQuarters() {
		return this.jpaSymbolFinancialsRepository.findCommonSfQuarters(Pageable.ofSize(20)).stream()
				.filter(myDto -> myDto.getTimesFound() >= 10).collect(Collectors.toList());
	}

	@Override
	public List<SymbolFinancials> findSymbolFinancials(SymbolFinancialsQueryParamsDto symbolFinancialsQueryParams) {
		List<SymbolFinancials> result = List.of();
		if (symbolFinancialsQueryParams.getFinancialElementParams() != null
				&& !symbolFinancialsQueryParams.getFinancialElementParams().isEmpty()
				&& (symbolFinancialsQueryParams.getSymbol() == null
						|| symbolFinancialsQueryParams.getSymbol().isBlank())
				&& (symbolFinancialsQueryParams.getQuarters() == null
						|| symbolFinancialsQueryParams.getQuarters().isEmpty())
				&& (symbolFinancialsQueryParams.getYearFilter() == null
						|| symbolFinancialsQueryParams.getYearFilter().getValue() == null
						|| 0 < BigDecimal.valueOf(1800)
								.compareTo(symbolFinancialsQueryParams.getYearFilter().getValue())
						|| symbolFinancialsQueryParams.getYearFilter().getOperation() == null)) {
			Set<FinancialElement> financialElements = this
					.findFinancialElements(symbolFinancialsQueryParams.getFinancialElementParams());
			final Map<SymbolFinancials, List<FinancialElement>> sfToFeMap = new HashMap<>();
			financialElements.forEach(myFe -> {
				this.entityManager.detach(myFe);
				this.entityManager.detach(myFe.getSymbolFinancials());
				if (!sfToFeMap.containsKey(myFe.getSymbolFinancials())) {
					sfToFeMap.put(myFe.getSymbolFinancials(), new ArrayList<>());
				}
				sfToFeMap.get(myFe.getSymbolFinancials()).add(myFe);
			});
			result = sfToFeMap.entrySet().stream().map(myEntry -> {
				myEntry.getKey().setFinancialElements(new HashSet<>(myEntry.getValue()));
				return myEntry.getKey();
			}).collect(Collectors.toList());
			return result;
		}

		final CriteriaQuery<SymbolFinancials> createQuery = this.entityManager.getCriteriaBuilder()
				.createQuery(SymbolFinancials.class);
		final Root<SymbolFinancials> root = createQuery.from(SymbolFinancials.class);
		root.fetch("financialElements");

		final List<Predicate> predicates = new ArrayList<>();
		if (symbolFinancialsQueryParams.getSymbol() != null && !symbolFinancialsQueryParams.getSymbol().isBlank()) {
			predicates.add(this.entityManager.getCriteriaBuilder().equal(
					this.entityManager.getCriteriaBuilder().lower(root.get("symbol")),
					symbolFinancialsQueryParams.getSymbol().trim().toLowerCase()));
		}
		if (symbolFinancialsQueryParams.getQuarters() != null && !symbolFinancialsQueryParams.getQuarters().isEmpty()) {
			predicates.add(this.entityManager.getCriteriaBuilder().in(root.get("quarter"))
					.value(symbolFinancialsQueryParams.getQuarters()));
		}
		if (symbolFinancialsQueryParams.getYearFilter() != null
				&& symbolFinancialsQueryParams.getYearFilter().getValue() != null
				&& 0 >= BigDecimal.valueOf(1800).compareTo(symbolFinancialsQueryParams.getYearFilter().getValue())
				&& symbolFinancialsQueryParams.getYearFilter().getOperation() != null) {
			if (Operation.SmallerEqual.equals(symbolFinancialsQueryParams.getYearFilter().getOperation())) {
				predicates.add(this.entityManager.getCriteriaBuilder().lessThanOrEqualTo(root.get("fiscalYear"),
						symbolFinancialsQueryParams.getYearFilter().getValue()));
			} else if (Operation.LargerEqual.equals(symbolFinancialsQueryParams.getYearFilter().getOperation())) {
				predicates.add(this.entityManager.getCriteriaBuilder().greaterThanOrEqualTo(root.get("fiscalYear"),
						symbolFinancialsQueryParams.getYearFilter().getValue()));
			} else if (Operation.Equal.equals(symbolFinancialsQueryParams.getYearFilter().getOperation())) {
				predicates.add(this.entityManager.getCriteriaBuilder().equal(root.get("fiscalYear"),
						symbolFinancialsQueryParams.getYearFilter().getValue()));
			}
		}
		Metamodel m = this.entityManager.getMetamodel();
		EntityType<SymbolFinancials> symbolFinancials_ = m.entity(SymbolFinancials.class);
		this.createFinancialElementClauses(symbolFinancialsQueryParams.getFinancialElementParams(), root, predicates,
				Optional.of(symbolFinancials_));
		if (!predicates.isEmpty()) {
			createQuery.where(predicates.toArray(new Predicate[0])).distinct(true);
		} else {
			return new LinkedList<>();
		}
		return this.entityManager.createQuery(createQuery).getResultStream().limit(40).collect(Collectors.toList());
	}

	private <T> void createFinancialElementClauses(List<FinancialElementParamDto> financialElementParamDtos,
			final Root<T> root, final List<Predicate> predicates,
			final Optional<EntityType<SymbolFinancials>> symbolFinancialsOpt) {
		final LinkedBlockingQueue<List<Predicate>> subPredicates = new LinkedBlockingQueue<List<Predicate>>();
		final LinkedBlockingQueue<DataHelper.Operation> operationArr = new LinkedBlockingQueue<DataHelper.Operation>();
		@SuppressWarnings("unchecked")
		final Path<FinancialElement> fePath = symbolFinancialsOpt.isPresent()
		? ((Root<SymbolFinancials>) root)
				.join(symbolFinancialsOpt.get().getDeclaredSet("financialElements", FinancialElement.class))
				: ((Root<FinancialElement>) root);
		if (financialElementParamDtos != null && !financialElementParamDtos.isEmpty()) {
			financialElementParamDtos.forEach(myDto -> {
				switch (myDto.getTermType()) {
				case StartTerm -> {
					try {
						operationArr.put(myDto.getOperation());
						subPredicates.put(new ArrayList<>());
					} catch (InterruptedException e) {
						new RuntimeException(e);
					}
				}
				case Query -> {
					financialElementConceptClause(fePath, operationArr.isEmpty() ? predicates : subPredicates.peek(),
							myDto);
					financialElementValueClause(fePath, operationArr.isEmpty() ? predicates : subPredicates.peek(), myDto);
				}
				case EndTerm -> {
					predicates.add(this.operatorClause(operationArr.poll(),
							subPredicates.poll().stream().toArray(x -> new Predicate[1])));
				}
				}
			});
		}
		// validate terms
		if (!operationArr.isEmpty() || !subPredicates.isEmpty()) {
			throw new RuntimeException(
					String.format("operationArr: %d, subPredicates: %d", operationArr.size(), subPredicates.size()));
		}
	}

	private Set<FinancialElement> findFinancialElements(List<FinancialElementParamDto> financialElementParams) {
		final CriteriaQuery<FinancialElement> createQuery = this.entityManager.getCriteriaBuilder()
				.createQuery(FinancialElement.class);
		final Root<FinancialElement> root = createQuery.from(FinancialElement.class);
		root.fetch("symbolFinancials");
		final List<Predicate> predicates = new ArrayList<>();
		this.createFinancialElementClauses(financialElementParams, root, predicates, Optional.empty());
		if (!predicates.isEmpty()) {
			createQuery.where(predicates.toArray(new Predicate[0])).distinct(true);
		} else {
			return new HashSet<>();
		}
		return new HashSet<>(this.entityManager.createQuery(createQuery).setMaxResults(10000).getResultList());
	}

	private <T> void financialElementValueClause(Path<FinancialElement> fePath, List<Predicate> predicates,
			FinancialElementParamDto myDto) {
		if (myDto.getValueFilter() != null && myDto.getValueFilter().getOperation() != null
				&& myDto.getValueFilter().getValue() != null
				&& (!BigDecimal.ZERO.equals(myDto.getValueFilter().getValue())
						&& !Operation.Equal.equals(myDto.getValueFilter().getOperation()))) {
			Expression<BigDecimal> joinPath = fePath.get("value");
			if (myDto.getValueFilter().getOperation().equals(Operation.Equal)) {
				Predicate equalPredicate = this.entityManager.getCriteriaBuilder().equal(joinPath,
						myDto.getValueFilter().getValue());
				predicates.add(this.operatorClause(myDto.getOperation(), equalPredicate));
			} else if (myDto.getValueFilter().getOperation().equals(Operation.SmallerEqual)) {
				Predicate lessThanOrEqualToPredicate = this.entityManager.getCriteriaBuilder()
						.lessThanOrEqualTo(joinPath, myDto.getValueFilter().getValue());
				predicates.add(this.operatorClause(myDto.getOperation(), lessThanOrEqualToPredicate));
			} else if (myDto.getValueFilter().getOperation().equals(Operation.LargerEqual)) {
				Predicate greaterThanOrEqualToPredicate = this.entityManager.getCriteriaBuilder()
						.greaterThanOrEqualTo(joinPath, myDto.getValueFilter().getValue());
				predicates.add(this.operatorClause(myDto.getOperation(), greaterThanOrEqualToPredicate));
			}
		}
	}

	private <T> void financialElementConceptClause(Path<FinancialElement> fePath, List<Predicate> predicates,
			FinancialElementParamDto myDto) {
		if (myDto.getConceptFilter().getOperation() != null && myDto.getConceptFilter().getValue() != null
				&& myDto.getConceptFilter().getValue().trim().length() > 2) {			
			Expression<String> lowerExp = this.entityManager.getCriteriaBuilder().lower(fePath.get("concept"));
			if (!myDto.getConceptFilter().getOperation()
					.equals(ch.xxx.manager.domain.model.dto.FilterStringDto.Operation.Equal)) {
				String filterStr = String.format("%%%s%%", myDto.getConceptFilter().getValue().trim().toLowerCase());
				if (myDto.getConceptFilter().getOperation()
						.equals(ch.xxx.manager.domain.model.dto.FilterStringDto.Operation.StartsWith)) {
					String.format("%s%%", myDto.getConceptFilter().getValue().trim().toLowerCase());
				} else if (myDto.getConceptFilter().getOperation()
						.equals(ch.xxx.manager.domain.model.dto.FilterStringDto.Operation.EndsWith)) {
					String.format("%%%s", myDto.getConceptFilter().getValue().trim().toLowerCase());
				}
				Predicate likePredicate = this.entityManager.getCriteriaBuilder().like(lowerExp, filterStr);
				predicates.add(operatorClause(myDto.getOperation(), likePredicate));
				predicates.forEach(myPredicate -> {
					LOGGER.info(myPredicate.getJavaType().getCanonicalName());
				});
			} else {
				Predicate equalPredicate = this.entityManager.getCriteriaBuilder().equal(lowerExp,
						myDto.getConceptFilter().getValue().trim().toLowerCase());
				predicates.add(this.operatorClause(myDto.getOperation(), equalPredicate));
			}
		}
	}

	private Predicate operatorClause(DataHelper.Operation operation, Predicate... likePredicate) {
		Predicate resultPredicate = null;
		if (operation.equals(DataHelper.Operation.And)) {
			resultPredicate = this.entityManager.getCriteriaBuilder().and(likePredicate);
		} else if (operation.equals(DataHelper.Operation.AndNot)) {
			resultPredicate = this.entityManager.getCriteriaBuilder()
					.not(this.entityManager.getCriteriaBuilder().and(likePredicate));
		} else if (operation.equals(DataHelper.Operation.Or)) {
			resultPredicate = this.entityManager.getCriteriaBuilder().or(likePredicate);
		} else if (operation.equals(DataHelper.Operation.OrNot)) {
			resultPredicate = this.entityManager.getCriteriaBuilder()
					.not(this.entityManager.getCriteriaBuilder().and(likePredicate));
		}
		return resultPredicate;
	}
}
