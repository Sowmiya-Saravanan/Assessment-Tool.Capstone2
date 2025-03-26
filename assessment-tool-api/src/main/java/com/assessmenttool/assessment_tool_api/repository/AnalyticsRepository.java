package com.assessmenttool.assessment_tool_api.repository;

import com.assessmenttool.assessment_tool_api.model.Analytics;
import com.assessmenttool.assessment_tool_api.model.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalyticsRepository extends JpaRepository<Analytics, Long> {

    List<Analytics> findByAssessmentAssessmentId(Long assessmentId);

    List<Analytics> findByClassIdClassId(Long classId);

    List<Analytics> findByMetricType(MetricType metricType);
}