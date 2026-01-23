import { Injectable } from '@angular/core';

/**
 * Service for handling dates in Indian Standard Time (IST - UTC+5:30)
 * This service ensures all dates across the application are consistently in IST
 */
@Injectable({
  providedIn: 'root'
})
export class DateUtilityService {

  // Indian timezone offset: UTC+5:30
  private readonly IST_OFFSET_HOURS = 5;
  private readonly IST_OFFSET_MINUTES = 30;
  private readonly IST_OFFSET_MS = (this.IST_OFFSET_HOURS * 60 + this.IST_OFFSET_MINUTES) * 60 * 1000;

  constructor() { }

  /**
   * Get current date in Indian timezone as YYYY-MM-DD format
   */
  getTodayInIST(): string {
    return this.getLocalDateString(new Date());
  }

  /**
   * Convert a Date object to YYYY-MM-DD format in Indian timezone
   * @param date - The date to convert
   * @returns Date string in YYYY-MM-DD format (IST)
   */
  getLocalDateString(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  /**
   * Convert a Date object to YYYY-MM-DD HH:MM:SS format in Indian timezone
   * @param date - The date to convert
   * @returns DateTime string in YYYY-MM-DD HH:MM:SS format (IST)
   */
  getLocalDateTimeString(date: Date): string {
    const dateStr = this.getLocalDateString(date);
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    return `${dateStr} ${hours}:${minutes}:${seconds}`;
  }

  /**
   * Convert date string to Date object (handles both date and datetime formats)
   * @param dateString - The date string (YYYY-MM-DD or YYYY-MM-DD HH:MM:SS)
   * @returns Date object
   */
  stringToDate(dateString: string): Date {
    return new Date(dateString);
  }

  /**
   * Format a date for display in Indian locale format
   * @param date - The date to format
   * @param format - Format type: 'short' (DD-MMM-YYYY), 'long' (DD MMMM YYYY), 'full' (Day, DD MMMM YYYY)
   * @returns Formatted date string
   */
  formatDate(date: Date | string, format: 'short' | 'long' | 'full' = 'short'): string {
    const d = typeof date === 'string' ? new Date(date) : date;
    
    const months = ['January', 'February', 'March', 'April', 'May', 'June',
      'July', 'August', 'September', 'October', 'November', 'December'];
    const days = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
    
    const day = String(d.getDate()).padStart(2, '0');
    const month = d.getMonth();
    const monthName = months[month];
    const monthShort = monthName.substring(0, 3);
    const year = d.getFullYear();
    const dayName = days[d.getDay()];

    switch (format) {
      case 'short':
        return `${day}-${monthShort}-${year}`;
      case 'long':
        return `${day} ${monthName} ${year}`;
      case 'full':
        return `${dayName}, ${day} ${monthName} ${year}`;
      default:
        return `${day}-${monthShort}-${year}`;
    }
  }

  /**
   * Add days to a date
   * @param date - Starting date
   * @param days - Number of days to add (can be negative)
   * @returns New Date object
   */
  addDays(date: Date, days: number): Date {
    const result = new Date(date);
    result.setDate(result.getDate() + days);
    return result;
  }

  /**
   * Add months to a date
   * @param date - Starting date
   * @param months - Number of months to add (can be negative)
   * @returns New Date object
   */
  addMonths(date: Date, months: number): Date {
    const result = new Date(date);
    result.setMonth(result.getMonth() + months);
    return result;
  }

  /**
   * Get date difference in days
   * @param date1 - First date
   * @param date2 - Second date
   * @returns Number of days between dates
   */
  getDaysDifference(date1: Date, date2: Date): number {
    const oneDay = 24 * 60 * 60 * 1000;
    return Math.round((date2.getTime() - date1.getTime()) / oneDay);
  }

  /**
   * Check if a date is today (in IST)
   * @param date - The date to check
   * @returns True if the date is today
   */
  isToday(date: Date | string): boolean {
    const d = typeof date === 'string' ? new Date(date) : date;
    const today = new Date();
    return this.getLocalDateString(d) === this.getLocalDateString(today);
  }

  /**
   * Check if a date is in the past
   * @param date - The date to check
   * @returns True if the date is in the past
   */
  isPast(date: Date | string): boolean {
    const d = typeof date === 'string' ? new Date(date) : date;
    return d.getTime() < new Date().getTime();
  }

  /**
   * Check if a date is in the future
   * @param date - The date to check
   * @returns True if the date is in the future
   */
  isFuture(date: Date | string): boolean {
    const d = typeof date === 'string' ? new Date(date) : date;
    return d.getTime() > new Date().getTime();
  }

  /**
   * Get start of day (00:00:00)
   * @param date - The date
   * @returns Date at 00:00:00
   */
  getStartOfDay(date: Date): Date {
    const d = new Date(date);
    d.setHours(0, 0, 0, 0);
    return d;
  }

  /**
   * Get end of day (23:59:59)
   * @param date - The date
   * @returns Date at 23:59:59
   */
  getEndOfDay(date: Date): Date {
    const d = new Date(date);
    d.setHours(23, 59, 59, 999);
    return d;
  }
}
