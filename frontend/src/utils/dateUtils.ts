// Date utility functions for the certification report system

/**
 * Set a date range from a specified number of days ago to today
 * @param daysAgo - Number of days in the past
 * @returns Object with startDate and endDate as ISO strings
 */
export const createDateRange = (daysAgo: number) => {
  const today = new Date();
  const pastDate = new Date(today);
  pastDate.setDate(today.getDate() - daysAgo);
  
  return {
    startDate: pastDate.toISOString().split('T')[0],
    endDate: today.toISOString().split('T')[0]
  };
};

/**
 * Calculate the number of days between two date strings
 * @param start - Start date as ISO string
 * @param end - End date as ISO string
 * @returns Number of days between the dates (inclusive)
 */
export const getDaysBetween = (start: string, end: string): number => {
  return Math.ceil((new Date(end).getTime() - new Date(start).getTime()) / (1000 * 60 * 60 * 24)) + 1;
};

/**
 * Format a date string for display
 * @param dateString - Date as ISO string
 * @returns Formatted date string
 */
export const formatDateDisplay = (dateString: string): string => {
  return new Date(dateString).toLocaleDateString('en-US', { 
    weekday: 'short', 
    month: 'short', 
    day: 'numeric', 
    year: 'numeric' 
  });
};
