package org.sdjen.download.cache_sis.store;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

@Service
public class Dao {
	@PersistenceContext
	private EntityManager em;

	@Transactional
	public <T> T merge(T entity) {
		return em.merge(entity);
	}

	public <T> T find(Class<T> clazz, Object key) {
		return em.find(clazz, key);
	}

	public <T> List<T> getList(String sql, Map<String, Object> params) {
		javax.persistence.Query query = em.createQuery(sql);
		if (null != params) {
			params.forEach((k, v) -> query.setParameter(k, v));
		}
		return query.getResultList();
	}

	@Transactional
	public int executeUpdate(String sql, Map<String, Object> params) {
		javax.persistence.Query query = em.createQuery(sql);
		if (null != params) {
			params.forEach((k, v) -> query.setParameter(k, v));
		}
		return query.executeUpdate();
	}
}
