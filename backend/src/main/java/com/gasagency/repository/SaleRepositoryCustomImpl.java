package com.gasagency.repository;

import com.gasagency.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class SaleRepositoryCustomImpl implements SaleRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Sale> findFilteredSalesCustom(LocalDate from, LocalDate to, Long customerId, Long variantId,
            Double minAmount, Double maxAmount, String referenceNumber, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Sale> cq = cb.createQuery(Sale.class);
        Root<Sale> sale = cq.from(Sale.class);
        sale.fetch("customer", JoinType.LEFT);
        sale.fetch("saleItems", JoinType.LEFT);
        sale.fetch("bankAccount", JoinType.LEFT);
        List<Predicate> predicates = new ArrayList<>();

        if (from != null) {
            predicates.add(cb.greaterThanOrEqualTo(sale.get("saleDate"), from));
        }
        if (to != null) {
            predicates.add(cb.lessThanOrEqualTo(sale.get("saleDate"), to));
        }
        if (customerId != null) {
            predicates.add(cb.equal(sale.get("customer").get("id"), customerId));
        }
        if (variantId != null) {
            Subquery<Long> subquery = cq.subquery(Long.class);
            Root<com.gasagency.entity.SaleItem> si2 = subquery.from(com.gasagency.entity.SaleItem.class);
            subquery.select(cb.literal(1L))
                    .where(cb.equal(si2.get("sale"), sale),
                            cb.equal(si2.get("variant").get("id"), variantId));
            predicates.add(cb.exists(subquery));
        }
        if (minAmount != null) {
            predicates.add(cb.greaterThanOrEqualTo(sale.get("totalAmount"), minAmount));
        }
        if (maxAmount != null) {
            predicates.add(cb.lessThanOrEqualTo(sale.get("totalAmount"), maxAmount));
        }
        if (referenceNumber != null && !referenceNumber.isEmpty()) {
            predicates.add(cb.like(sale.get("referenceNumber"), "%" + referenceNumber + "%"));
        }

        cq.select(sale).distinct(true).where(predicates.toArray(new Predicate[0]));

        TypedQuery<Sale> query = entityManager.createQuery(cq);
        List<Sale> resultList;
        if (pageable.isUnpaged()) {
            resultList = query.getResultList();
        } else {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
            resultList = query.getResultList();
        }

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Sale> countRoot = countQuery.from(Sale.class);
        List<Predicate> countPredicates = new ArrayList<>();
        if (from != null)
            countPredicates.add(cb.greaterThanOrEqualTo(countRoot.get("saleDate"), from));
        if (to != null)
            countPredicates.add(cb.lessThanOrEqualTo(countRoot.get("saleDate"), to));
        if (customerId != null)
            countPredicates.add(cb.equal(countRoot.get("customer").get("id"), customerId));
        if (variantId != null) {
            Subquery<Long> subquery = countQuery.subquery(Long.class);
            Root<com.gasagency.entity.SaleItem> si2 = subquery.from(com.gasagency.entity.SaleItem.class);
            subquery.select(cb.literal(1L))
                    .where(cb.equal(si2.get("sale"), countRoot),
                            cb.equal(si2.get("variant").get("id"), variantId));
            countPredicates.add(cb.exists(subquery));
        }
        if (minAmount != null)
            countPredicates.add(cb.greaterThanOrEqualTo(countRoot.get("totalAmount"), minAmount));
        if (maxAmount != null)
            countPredicates.add(cb.lessThanOrEqualTo(countRoot.get("totalAmount"), maxAmount));
        if (referenceNumber != null && !referenceNumber.isEmpty())
            countPredicates.add(cb.like(countRoot.get("referenceNumber"), "%" + referenceNumber + "%"));
        countQuery.select(cb.countDistinct(countRoot)).where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(resultList, pageable, total);
    }

    @Override
    public Page<Sale> findByDateRange(LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Sale> cq = cb.createQuery(Sale.class);
        Root<Sale> sale = cq.from(Sale.class);
        sale.fetch("customer", JoinType.LEFT);
        sale.fetch("saleItems", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.greaterThanOrEqualTo(sale.get("saleDate"), fromDate));
        predicates.add(cb.lessThanOrEqualTo(sale.get("saleDate"), toDate));

        cq.select(sale).distinct(true).where(predicates.toArray(new Predicate[0]))
                .orderBy(cb.desc(sale.get("saleDate")), cb.desc(sale.get("id")));

        TypedQuery<Sale> query = entityManager.createQuery(cq);
        List<Sale> resultList;
        if (pageable.isUnpaged()) {
            resultList = query.getResultList();
        } else {
            query.setFirstResult((int) pageable.getOffset());
            query.setMaxResults(pageable.getPageSize());
            resultList = query.getResultList();
        }

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Sale> countRoot = countQuery.from(Sale.class);
        countQuery.select(cb.countDistinct(countRoot))
                .where(cb.greaterThanOrEqualTo(countRoot.get("saleDate"), fromDate),
                        cb.lessThanOrEqualTo(countRoot.get("saleDate"), toDate));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(resultList, pageable, total);
    }
}
