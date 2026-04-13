/**
 * Sleep Report Generator
 * Generates formatted sleep analysis reports for user consumption
 */

import type { SleepAnalysisResult, SleepRecord, SleepStage, SnoreAnalysis } from './sleep-analyzer';

export interface SleepReport {
  id: string;
  user_id: string;
  date: string;
  summary: ReportSummary;
  quality: QualitySection;
  stages: StagesSection;
  snoring: SnoringSection;
  recommendations: RecommendationSection;
  generated_at: string;
}

export interface ReportSummary {
  title: string;
  sleep_duration_formatted: string;
  sleep_efficiency: number;
  overall_score: number;
  score_label: 'Excellent' | 'Good' | 'Fair' | 'Poor';
}

export interface QualitySection {
  score: number;
  breakdown: {
    duration_score: number;
    stages_score: number;
    snoring_score: number;
  };
  description: string;
}

export interface StagesSection {
  stages: Array<{
    name: string;
    duration_formatted: string;
    percentage: number;
    icon: string;
  }>;
  total_sleep_formatted: string;
  notes: string;
}

export interface SnoringSection {
  total_episodes: number;
  total_duration_formatted: string;
  severity: SnoreAnalysis['pattern'];
  severity_description: string;
  trend: 'improving' | 'stable' | 'worsening';
}

export interface RecommendationSection {
  items: Array<{
    priority: 'high' | 'medium' | 'low';
    category: 'duration' | 'quality' | 'snoring' | 'lifestyle' | 'medical';
    text: string;
    action: string;
  }>;
}

function formatDuration(minutes: number): string {
  const hours = Math.floor(minutes / 60);
  const mins = minutes % 60;
  if (hours === 0) return `${mins}m`;
  if (mins === 0) return `${hours}h`;
  return `${hours}h ${mins}m`;
}

function formatTimeRange(startTime: number, endTime: number): string {
  const start = new Date(startTime);
  const end = new Date(endTime);
  const formatTime = (d: Date) => d.toLocaleTimeString('en-US', {
    hour: 'numeric',
    minute: '2-digit',
    hour12: true
  });
  return `${formatTime(start)} - ${formatTime(end)}`;
}

function getScoreLabel(score: number): ReportSummary['score_label'] {
  if (score >= 85) return 'Excellent';
  if (score >= 70) return 'Good';
  if (score >= 50) return 'Fair';
  return 'Poor';
}

function getQualityDescription(score: number): string {
  if (score >= 85) {
    return 'Outstanding sleep quality. Your sleep patterns show excellent recovery and restoration.';
  }
  if (score >= 70) {
    return 'Good sleep quality with room for improvement. Most sleep stages are well-balanced.';
  }
  if (score >= 50) {
    return 'Fair sleep quality. Consider addressing factors affecting your sleep duration and stages.';
  }
  return 'Poor sleep quality detected. Multiple areas need attention for optimal sleep health.';
}

function getSnoringSeverityDescription(pattern: SnoreAnalysis['pattern']): string {
  switch (pattern) {
    case 'minimal':
      return 'Minimal snoring detected. This is normal and typically not a health concern.';
    case 'moderate':
      return 'Moderate snoring patterns observed. Some lifestyle changes may help reduce frequency.';
    case 'frequent':
      return 'Frequent snoring episodes detected. Consider sleep position adjustments and lifestyle factors.';
    case 'severe':
      return 'Severe snoring patterns. Medical evaluation for sleep apnea is recommended.';
    default:
      return 'Unable to determine snoring severity.';
  }
}

function categorizeRecommendation(text: string): RecommendationSection['items'][0]['category'] {
  const lower = text.toLowerCase();
  if (lower.includes('snoring') || lower.includes('snore') || lower.includes('apnea')) {
    return 'snoring';
  }
  if (lower.includes('hour') || lower.includes('sleep duration') || lower.includes('schedule')) {
    return 'duration';
  }
  if (lower.includes('deep sleep') || lower.includes('rem') || lower.includes('stage')) {
    return 'quality';
  }
  if (lower.includes('consult') || lower.includes('doctor') || lower.includes('medical') || lower.includes('specialist')) {
    return 'medical';
  }
  return 'lifestyle';
}

function determinePriority(text: string): RecommendationSection['items'][0]['priority'] {
  const lower = text.toLowerCase();
  if (lower.includes('consult') || lower.includes('medical') || lower.includes('apnea') || lower.includes('specialist')) {
    return 'high';
  }
  if (lower.includes('consider') || lower.includes('improve')) {
    return 'medium';
  }
  return 'low';
}

function generateStageNotes(stages: SleepStage[]): string {
  const deepSleep = stages.find(s => s.stage === 'deep');
  const remSleep = stages.find(s => s.stage === 'rem');
  const lightSleep = stages.find(s => s.stage === 'light');

  const notes: string[] = [];

  if (deepSleep) {
    if (deepSleep.percentage >= 15 && deepSleep.percentage <= 25) {
      notes.push('Your deep sleep duration is within the optimal range, supporting good physical recovery.');
    } else if (deepSleep.percentage < 15) {
      notes.push('Deep sleep is below optimal levels. This may affect physical recovery and immune function.');
    } else {
      notes.push('Deep sleep is above average, which is generally beneficial for physical restoration.');
    }
  }

  if (remSleep) {
    if (remSleep.percentage >= 20 && remSleep.percentage <= 25) {
      notes.push('REM sleep is well-balanced, supporting good cognitive function and memory consolidation.');
    } else if (remSleep.percentage < 20) {
      notes.push('REM sleep could be improved. This may impact mood regulation and memory.');
    }
  }

  if (lightSleep) {
    notes.push('Light sleep serves as a transition phase between other sleep stages.');
  }

  return notes.join(' ');
}

export function generateReport(
  record: SleepRecord,
  analysis: SleepAnalysisResult
): SleepReport {
  const { sleep_stages, snore_analysis, quality_score, sleep_efficiency } = analysis;

  const summary: ReportSummary = {
    title: `Sleep Report - ${record.date}`,
    sleep_duration_formatted: formatDuration(record.total_duration_minutes),
    sleep_efficiency,
    overall_score: quality_score,
    score_label: getScoreLabel(quality_score),
  };

  const durationScore = record.total_duration_minutes >= 420 && record.total_duration_minutes <= 540 ? 100 :
    record.total_duration_minutes >= 360 && record.total_duration_minutes <= 600 ? 80 : 60;
  const deepSleepScore = sleep_stages.find(s => s.stage === 'deep')?.percentage || 0;
  const stagesScore = Math.min(100, (deepSleepScore / 20) * 100);
  const snoringScore = 100 - snore_analysis.severity_score;

  const quality: QualitySection = {
    score: quality_score,
    breakdown: {
      duration_score: durationScore,
      stages_score: Math.round(stagesScore),
      snoring_score: snoringScore,
    },
    description: getQualityDescription(quality_score),
  };

  const stageIcons: Record<string, string> = {
    deep: '🌙',
    rem: '🧠',
    light: '☁️',
    awake: '⏰',
  };

  const stagesSection: StagesSection = {
    stages: sleep_stages.map(stage => ({
      name: stage.stage.charAt(0).toUpperCase() + stage.stage.slice(1),
      duration_formatted: formatDuration(stage.duration_minutes),
      percentage: stage.percentage,
      icon: stageIcons[stage.stage] || '💤',
    })),
    total_sleep_formatted: formatDuration(record.total_duration_minutes),
    notes: generateStageNotes(sleep_stages),
  };

  const snoringSection: SnoringSection = {
    total_episodes: snore_analysis.total_episodes,
    total_duration_formatted: formatDuration(snore_analysis.total_duration_minutes),
    severity: snore_analysis.pattern,
    severity_description: getSnoringSeverityDescription(snore_analysis.pattern),
    trend: 'stable',
  };

  const recommendationsSection: RecommendationSection = {
    items: analysis.recommendations.map(text => ({
      priority: determinePriority(text),
      category: categorizeRecommendation(text),
      text,
      action: `Follow recommendation: ${text}`,
    })).sort((a, b) => {
      const priorityOrder = { high: 0, medium: 1, low: 2 };
      return priorityOrder[a.priority] - priorityOrder[b.priority];
    }),
  };

  return {
    id: `sleep-report-${record.user_id}-${record.date}`,
    user_id: record.user_id,
    date: record.date,
    summary,
    quality,
    stages: stagesSection,
    snoring: snoringSection,
    recommendations: recommendationsSection,
    generated_at: new Date().toISOString(),
  };
}

export function generateMarkdownReport(
  record: SleepRecord,
  analysis: SleepAnalysisResult
): string {
  const report = generateReport(record, analysis);

  let md = `# ${report.summary.title}\n\n`;
  md += `**Overall Score:** ${report.summary.overall_score}/100 (${report.summary.score_label})\n`;
  md += `**Sleep Duration:** ${report.summary.sleep_duration_formatted}\n`;
  md += `**Sleep Efficiency:** ${report.summary.sleep_efficiency}%\n\n`;

  md += `## Sleep Quality Breakdown\n\n`;
  md += `| Factor | Score |\n`;
  md += `|--------|-------|\n`;
  md += `| Duration | ${report.quality.breakdown.duration_score}/100 |\n`;
  md += `| Sleep Stages | ${report.quality.breakdown.stages_score}/100 |\n`;
  md += `| Snoring Impact | ${report.quality.breakdown.snoring_score}/100 |\n\n`;
  md += `${report.quality.description}\n\n`;

  md += `## Sleep Stages\n\n`;
  md += `| Stage | Duration | Percentage |\n`;
  md += `|-------|----------|------------|\n`;
  for (const stage of report.stages.stages) {
    md += `| ${stage.icon} ${stage.name} | ${stage.duration_formatted} | ${stage.percentage}% |\n`;
  }
  md += `\n${report.stages.notes}\n\n`;

  md += `## Snoring Analysis\n\n`;
  md += `**Total Episodes:** ${report.snoring.total_episodes}\n`;
  md += `**Total Duration:** ${report.snoring.total_duration_formatted}\n`;
  md += `**Severity:** ${report.snoring.severity.charAt(0).toUpperCase() + report.snoring.severity.slice(1)}\n\n`;
  md += `${report.snoring.severity_description}\n\n`;

  md += `## Recommendations\n\n`;
  for (const rec of report.recommendations.items) {
    const priorityEmoji = rec.priority === 'high' ? '🔴' : rec.priority === 'medium' ? '🟡' : '🟢';
    md += `${priorityEmoji} **[${rec.category.toUpperCase()}]** ${rec.text}\n`;
  }

  md += `\n---\n`;
  md += `*Report generated at ${new Date(report.generated_at).toLocaleString()}*\n`;

  return md;
}
