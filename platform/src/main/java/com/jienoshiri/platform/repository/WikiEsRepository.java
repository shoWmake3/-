package com.jienoshiri.platform.repository;
import com.jienoshiri.platform.document.WikiDoc;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WikiEsRepository extends ElasticsearchRepository<WikiDoc, Long> {}