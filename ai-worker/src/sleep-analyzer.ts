/**
 * Sleep Pattern Analyzer
 * Analyzes sleep records using Workers AI for pattern recognition and health insights
 */

import { createWorkersAI } from 'workers-ai-provider';
import { generateText } from 'ai';

type Env = {
  AI: Fetcher;
};

// ── Types ─────────────────────────────────────────────────────────────────────

export interface SleepEvent {
  timestamp: number; // Unix timestamp in ms
  type: 'snore' | 'movement' | 'heart_rate' | 'breathing';
  value: number;
  duration_ms?: number;
}

export interface SleepRecord {
  user_id: string;
  date: string; // ISO date string YYYY-MM-DD
  start_time: number; // Unix timestamp in ms
  end_time: number; // Unix timestamp in ms
  events: SleepEvent[];
  total_duration_minutes: number;
  heart_rate_avg?: number;
  breathing_rate_avg?: number;
}

export interface SleepStage {
  stage: 'deep' | 'light' | 'rem' | 'awake';
  duration_minutes: number;
  percentage: number;
  start_time: number;
  end_time: number;
}

export interface SnoreAnalysis {
  total_episodes: number;
  total_duration_minutes: number;
  avg_episode_duration_seconds: number;
  severity_score: number; // 0-100
  pattern: 'minimal' | 'moderate' | 'frequent' | 'severe';
  peak_hours: number[]; // Hours of day (0-23) with most snore events
}

export interface SleepAnalysisResult {
  quality_score: number; // 0-100
  sleep_stages: SleepStage[];
  snore_analysis: SnoreAnalysis;
  total_sleep_minutes: number;
  sleep_efficiency: number; // Percentage of time in bed spent sleeping
  recommendations: string[];
  insights: string[];
  analyzed_at: string;
}

// ── Sleep Stage Detection ─────────────────────────────────────────────────────

/**
 * Detect sleep stages based on heart rate and movement patterns
 * Uses heuristic-based analysis since we don't have EEG data
 */
export function detectSleepStages(record: SleepRecord): SleepStage[] {
  const stages: SleepStage[] = [];
  const durationMs = record.end_time - record.start_time;
  const stageDurationMs = durationMs / 4; // 4 stages per night approximation

  // Movement-based stage estimation
  const movements = record.events.filter(e => e.type === 'movement');
  const hrAvg = record.heart_rate_avg || 65;

  // Deep sleep typically has lower HR and fewer movements
  // REM has variable HR with dreaming
  // Light sleep has moderate HR
  // Awake periods have highest HR and most movements

  for (let i = 0; i < 4; i++) {
    const stageStart = record.start_time + (i * stageDurationMs);
    const stageEnd = stageStart + stageDurationMs;
    const stageEvents = movements.filter(
      e => e.timestamp >= stageStart && e.timestamp < stageEnd
    );
    const movementDensity = stageEvents.length / (stageDurationMs / 60000); // movements per minute

    let stage: SleepStage['stage'];
    let durationMinutes = stageDurationMs / 60000;

    if (i === 0 || i === 3) {
      // First and last quarters tend to have more REM
      stage = movementDensity < 0.5 ? 'rem' : 'light';
    } else if (movementDensity < 0.3 && hrAvg < 60) {
      // Low movement + low HR = deep sleep
      stage = 'deep';
    } else if (movementDensity > 1.5) {
      // High movement = awake
      stage = 'awake';
    } else {
      stage = 'light';
    }

    stages.push({
      stage,
      duration_minutes: Math.round(durationMinutes),
      percentage: Math.round((durationMinutes / (durationMs / 60000)) * 100),
      start_time: stageStart,
      end_time: stageEnd,
    });
  }

  return stages;
}

// ── Snore Analysis ────────────────────────────────────────────────────────────

/**
 * Analyze snoring patterns from sleep events
 */
export function analyzeSnoring(record: SleepRecord): SnoreAnalysis {
  const snoreEvents = record.events.filter(e => e.type === 'snore');

  if (snoreEvents.length === 0) {
    return {
      total_episodes: 0,
      total_duration_minutes: 0,
      avg_episode_duration_seconds: 0,
      severity_score: 0,
      pattern: 'minimal',
      peak_hours: [],
    };
  }

  // Group snore events into episodes (events within 30 seconds = same episode)
  const episodes: SleepEvent[][] = [];
  let currentEpisode: SleepEvent[] = [];

  const sortedEvents = [...snoreEvents].sort((a, b) => a.timestamp - b.timestamp);

  for (const event of sortedEvents) {
    if (currentEpisode.length === 0) {
      currentEpisode.push(event);
    } else {
      const lastEvent = currentEpisode[currentEpisode.length - 1];
      if (event.timestamp - lastEvent.timestamp < 30000) {
        currentEpisode.push(event);
      } else {
        episodes.push(currentEpisode);
        currentEpisode = [event];
      }
    }
  }
  if (currentEpisode.length > 0) {
    episodes.push(currentEpisode);
  }

  // Calculate episode statistics
  const episodeDurations = episodes.map(ep => {
    const start = ep[0].timestamp;
    const end = ep[ep.length - 1].timestamp + (ep[ep.length - 1].duration_ms || 0);
    return (end - start) / 1000; // seconds
  });

  const totalDuration = episodeDurations.reduce((sum, d) => sum + d, 0);
  const avgDuration = episodeDurations.length > 0
    ? totalDuration / episodeDurations.length
    : 0;

  // Calculate severity score (0-100)
  // Based on: frequency, duration, and intensity of snore events
  const frequencyScore = Math.min(30, snoreEvents.length * 2);
  const durationScore = Math.min(30, (totalDuration / 60) * 3); // minutes of snoring
  const intensityScore = Math.min(40, snoreEvents.reduce((sum, e) => sum + e.value, 0) / 10);
  const severityScore = Math.round(frequencyScore + durationScore + intensityScore);

  // Determine pattern
  let pattern: SnoreAnalysis['pattern'];
  if (severityScore < 20) pattern = 'minimal';
  else if (severityScore < 40) pattern = 'moderate';
  else if (severityScore < 70) pattern = 'frequent';
  else pattern = 'severe';

  // Find peak hours
  const hourCounts: Record<number, number> = {};
  for (const event of snoreEvents) {
    const date = new Date(event.timestamp);
    const hour = date.getHours();
    hourCounts[hour] = (hourCounts[hour] || 0) + 1;
  }
  const peakHours = Object.entries(hourCounts)
    .filter(([, count]) => count > snoreEvents.length * 0.15) // More than 15% of events
    .map(([hour]) => parseInt(hour))
    .sort((a, b) => hourCounts[b] - hourCounts[a]);

  return {
    total_episodes: episodes.length,
    total_duration_minutes: Math.round(totalDuration / 60),
    avg_episode_duration_seconds: Math.round(avgDuration),
    severity_score: severityScore,
    pattern,
    peak_hours: peakHours.slice(0, 3), // Top 3 peak hours
  };
}

// ── Quality Score Calculation ─────────────────────────────────────────────────

/**
 * Calculate overall sleep quality score (0-100)
 */
export function calculateQualityScore(
  record: SleepRecord,
  stages: SleepStage[],
  snoreAnalysis: SnoreAnalysis
): number {
  let score = 100;

  // Duration scoring (ideal: 7-9 hours = 420-540 minutes)
  const sleepMinutes = record.total_duration_minutes;
  if (sleepMinutes < 360) score -= 30; // Less than 6 hours
  else if (sleepMinutes < 420) score -= 15; // 6-7 hours
  else if (sleepMinutes > 600) score -= 10; // More than 10 hours

  // Deep sleep scoring (ideal: 15-25% of total)
  const deepSleep = stages.find(s => s.stage === 'deep');
  if (deepSleep) {
    const deepPercentage = deepSleep.percentage;
    if (deepPercentage < 10) score -= 20;
    else if (deepPercentage < 15) score -= 10;
    else if (deepPercentage > 30) score -= 5; // Unusually high might indicate issues
  } else {
    score -= 25; // No detected deep sleep
  }

  // REM scoring (ideal: 20-25% of total)
  const remSleep = stages.find(s => s.stage === 'rem');
  if (remSleep) {
    const remPercentage = remSleep.percentage;
    if (remPercentage < 15) score -= 10;
    else if (remPercentage > 30) score -= 5;
  }

  // Snoring impact
  if (snoreAnalysis.pattern === 'severe') score -= 30;
  else if (snoreAnalysis.pattern === 'frequent') score -= 20;
  else if (snoreAnalysis.pattern === 'moderate') score -= 10;

  // Awake time impact
  const awakeTime = stages
    .filter(s => s.stage === 'awake')
    .reduce((sum, s) => sum + s.duration_minutes, 0);
  if (awakeTime > 60) score -= 15;
  else if (awakeTime > 30) score -= 10;
  else if (awakeTime > 15) score -= 5;

  return Math.max(0, Math.min(100, score));
}

// ── Health Recommendations ────────────────────────────────────────────────────

/**
 * Generate health recommendations based on sleep analysis
 */
export function generateRecommendations(
  record: SleepRecord,
  stages: SleepStage[],
  snoreAnalysis: SnoreAnalysis,
  qualityScore: number
): string[] {
  const recommendations: string[] = [];

  // Duration recommendations
  if (record.total_duration_minutes < 420) {
    recommendations.push('Try to get at least 7 hours of sleep for optimal recovery.');
  } else if (record.total_duration_minutes > 540) {
    recommendations.push('Your sleep duration is above average. Ensure you maintain consistent wake times.');
  }

  // Deep sleep recommendations
  const deepSleep = stages.find(s => s.stage === 'deep');
  if (deepSleep && deepSleep.percentage < 15) {
    recommendations.push('Consider improving deep sleep by avoiding screens 1 hour before bed and keeping your bedroom cool (65-68°F/18-20°C).');
  }

  // REM sleep recommendations
  const remSleep = stages.find(s => s.stage === 'rem');
  if (remSleep && remSleep.percentage < 18) {
    recommendations.push('To improve REM sleep, maintain a consistent sleep schedule and manage stress before bed.');
  }

  // Snoring recommendations
  if (snoreAnalysis.pattern === 'moderate' || snoreAnalysis.pattern === 'frequent') {
    recommendations.push('Consider sleeping on your side and elevating your head to reduce snoring.');
  }
  if (snoreAnalysis.pattern === 'severe') {
    recommendations.push('Frequent snoring may indicate sleep apnea. Please consult a healthcare provider for evaluation.');
  }

  // Heart rate recommendations
  if (record.heart_rate_avg && record.heart_rate_avg > 80) {
    recommendations.push('Your resting heart rate during sleep is elevated. Consider relaxation techniques before bed.');
  }

  // General recommendations based on quality score
  if (qualityScore < 50) {
    recommendations.push('Your sleep quality needs attention. Review sleep hygiene practices and consider speaking with a sleep specialist.');
  } else if (qualityScore >= 80) {
    recommendations.push('Great sleep quality! Keep maintaining your current sleep habits.');
  }

  return recommendations;
}

// ── AI-Enhanced Analysis ─────────────────────────────────────────────────────

/**
 * Use Workers AI to generate additional insights and personalized recommendations
 */
export async function enhanceWithAI(
  record: SleepRecord,
  analysis: Omit<SleepAnalysisResult, 'recommendations' | 'insights' | 'analyzed_at'>,
  env: Env
): Promise<Pick<SleepAnalysisResult, 'recommendations' | 'insights'>> {
  try {
    const workersai = createWorkersAI({ binding: env.AI as any });

    const prompt = `Analyze this sleep data and provide health insights:

Sleep Record:
- Date: ${record.date}
- Duration: ${record.total_duration_minutes} minutes
- Heart Rate Average: ${record.heart_rate_avg || 'N/A'} bpm
- Breathing Rate Average: ${record.breathing_rate_avg || 'N/A'} breaths/min
- Snore Events: ${analysis.snore_analysis.total_episodes} episodes
- Snore Duration: ${analysis.snore_analysis.total_duration_minutes} minutes
- Sleep Stages: ${analysis.sleep_stages.map(s => `${s.stage}(${s.percentage}%)`).join(', ')}

Quality Score: ${analysis.quality_score}/100

Provide exactly 3-5 short, actionable health recommendations and 2-3 key insights in JSON format:
{
  "recommendations": ["recommendation 1", "recommendation 2", ...],
  "insights": ["insight 1", "insight 2", ...]
}

Keep recommendations practical and evidence-based. Format as a valid JSON object only.`;

    const result = await generateText({
      model: workersai('@cf/mistralai/mistral-small-3.1-24b-instruct') as any,
      messages: [{ role: 'user', content: prompt }] as any,
      maxOutputTokens: 500,
    });

    const text = result.text?.trim() || '{}';
    // Extract JSON from response
    const jsonMatch = text.match(/\{[\s\S]*\}/);
    if (jsonMatch) {
      try {
        const parsed = JSON.parse(jsonMatch[0]);
        return {
          recommendations: Array.isArray(parsed.recommendations) ? parsed.recommendations : [],
          insights: Array.isArray(parsed.insights) ? parsed.insights : [],
        };
      } catch {
        // Fall back to basic recommendations if parsing fails
      }
    }
  } catch (error) {
    console.error('AI enhancement failed:', error);
  }

  // Return basic recommendations if AI fails
  return {
    recommendations: generateRecommendations(record, analysis.sleep_stages, analysis.snore_analysis, analysis.quality_score),
    insights: [
      `You achieved ${analysis.quality_score}% sleep quality for this record.`,
      `${analysis.sleep_stages.length > 0 ? `Your deepest sleep occurred during ${analysis.sleep_stages.find(s => s.stage === 'deep')?.duration_minutes || 0} minutes of deep sleep.` : 'Track more nights for stage analysis.'}`,
    ],
  };
}

// ── Main Analysis Function ─────────────────────────────────────────────────────

/**
 * Analyze sleep record and return comprehensive results
 */
export async function analyzeSleep(
  record: SleepRecord,
  env: Env,
  useAIEnhancement: boolean = true
): Promise<SleepAnalysisResult> {
  // Detect sleep stages
  const stages = detectSleepStages(record);

  // Analyze snoring patterns
  const snoreAnalysis = analyzeSnoring(record);

  // Calculate quality score
  const qualityScore = calculateQualityScore(record, stages, snoreAnalysis);

  // Calculate sleep efficiency
  const totalTimeMs = record.end_time - record.start_time;
  const sleepTimeMs = record.total_duration_minutes * 60000;
  const sleepEfficiency = Math.round((sleepTimeMs / totalTimeMs) * 100);

  // Build initial analysis
  const initialAnalysis = {
    quality_score: qualityScore,
    sleep_stages: stages,
    snore_analysis: snoreAnalysis,
    total_sleep_minutes: record.total_duration_minutes,
    sleep_efficiency: sleepEfficiency,
  };

  // Generate basic recommendations
  const basicRecommendations = generateRecommendations(
    record,
    stages,
    snoreAnalysis,
    qualityScore
  );

  // Enhance with AI if enabled
  let recommendations = basicRecommendations;
  let insights: string[] = [];

  if (useAIEnhancement) {
    const enhanced = await enhanceWithAI(record, initialAnalysis, env);
    recommendations = enhanced.recommendations.length > 0
      ? enhanced.recommendations
      : basicRecommendations;
    insights = enhanced.insights;
  }

  // Default insights if none from AI
  if (insights.length === 0) {
    insights = [
      `Your sleep quality score is ${qualityScore}/100.`,
      `Total snoring time: ${snoreAnalysis.total_duration_minutes} minutes across ${snoreAnalysis.total_episodes} episodes.`,
      `Sleep efficiency: ${sleepEfficiency}% of time in bed was spent sleeping.`,
    ];
  }

  return {
    ...initialAnalysis,
    recommendations,
    insights,
    analyzed_at: new Date().toISOString(),
  };
}

// ── Validation ────────────────────────────────────────────────────────────────

/**
 * Validate sleep record input
 */
export function validateSleepRecord(record: unknown): { valid: boolean; error?: string } {
  if (!record || typeof record !== 'object') {
    return { valid: false, error: 'Invalid request body' };
  }

  const r = record as Record<string, unknown>;

  if (!r.user_id || typeof r.user_id !== 'string') {
    return { valid: false, error: 'Missing or invalid user_id' };
  }

  if (!r.date || typeof r.date !== 'string') {
    return { valid: false, error: 'Missing or invalid date' };
  }

  if (!r.start_time || typeof r.start_time !== 'number') {
    return { valid: false, error: 'Missing or invalid start_time' };
  }

  if (!r.end_time || typeof r.end_time !== 'number') {
    return { valid: false, error: 'Missing or invalid end_time' };
  }

  if (r.end_time <= r.start_time) {
    return { valid: false, error: 'end_time must be after start_time' };
  }

  if (!Array.isArray(r.events)) {
    return { valid: false, error: 'Missing or invalid events array' };
  }

  for (const event of r.events as unknown[]) {
    if (!event || typeof event !== 'object') {
      return { valid: false, error: 'Invalid event in events array' };
    }
    const e = event as Record<string, unknown>;
    if (typeof e.timestamp !== 'number') {
      return { valid: false, error: 'Invalid event timestamp' };
    }
    if (!['snore', 'movement', 'heart_rate', 'breathing'].includes(e.type as string)) {
      return { valid: false, error: `Invalid event type: ${e.type}` };
    }
  }

  return { valid: true };
}
