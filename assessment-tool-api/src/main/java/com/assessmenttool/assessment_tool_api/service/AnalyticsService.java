// package com.assessmenttool.assessment_tool_api.service;

// import com.assessmenttool.assessment_tool_api.model.Analytics;
// import com.assessmenttool.assessment_tool_api.model.MetricType;
// import com.assessmenttool.assessment_tool_api.repository.AnalyticsRepository;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.stereotype.Service;

// import java.util.List;

// @Service
// public class AnalyticsService {

//     @Autowired
//     private AnalyticsRepository analyticsRepository;

//     public List<Analytics> getAssessmentAnalytics() {
//         List<Analytics> analytics = analyticsRepository.findAll();
//         if (analytics.isEmpty()) {
//             throw new IllegalStateException("No analytics data available");
//         }
//         return analytics;
//     }

//     public List<Analytics> getAnalyticsByAssessment(Long assessmentId) {
//         if (assessmentId == null || assessmentId <= 0) {
//             throw new IllegalArgumentException("Invalid assessment ID");
//         }
//         List<Analytics> analytics = analyticsRepository.findByAssessmentAssessmentId(assessmentId);
//         if (analytics.isEmpty()) {
//             throw new IllegalStateException("No analytics data available for assessment ID: " + assessmentId);
//         }
//         return analytics;
//     }

//     public List<Analytics> getAnalyticsByClass(Long classId) {
//         if (classId == null || classId <= 0) {
//             throw new IllegalArgumentException("Invalid class ID");
//         }
//         List<Analytics> analytics = analyticsRepository.findByClassIdClassId(classId);
//         if (analytics.isEmpty()) {
//             throw new IllegalStateException("No analytics data available for class ID: " + classId);
//         }
//         return analytics;
//     }

//     public List<Analytics> getAnalyticsByMetricType(MetricType metricType) {
//         if (metricType == null) {
//             throw new IllegalArgumentException("Metric type is required");
//         }
//         List<Analytics> analytics = analyticsRepository.findByMetricType(metricType);
//         if (analytics.isEmpty()) {
//             throw new IllegalStateException("No analytics data available for metric type: " + metricType);
//         }
//         return analytics;
//     }
// }