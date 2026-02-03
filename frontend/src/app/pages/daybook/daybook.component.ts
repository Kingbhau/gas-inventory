import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faPrint, faRefresh, faClipboard, faDownload } from '@fortawesome/free-solid-svg-icons';
import { ToastrService } from 'ngx-toastr';
import { DayBookService } from '../../services/daybook.service';
import { LoadingService } from '../../services/loading.service';
import { SharedModule } from '../../shared/shared.module';
import { finalize } from 'rxjs';
import { exportDayBookReportToPDF } from './export-daybook-report.util';

@Component({
  selector: 'app-daybook',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, FontAwesomeModule, SharedModule],
  templateUrl: './daybook.component.html',
  styleUrl: './daybook.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DayBookComponent implements OnInit, OnDestroy {
  // Font Awesome Icons
  faPrint = faPrint;
  faRefresh = faRefresh;
  faClipboard = faClipboard;
  faDownload = faDownload;

  // Data
  dayBookTransactions: any[] = [];
  dayBookSummary: any = null;
  selectedDate: string = '';
  isLoading = false;

  // Pagination
  dayBookPage = 1;
  dayBookPageSize = 10;
  dayBookTotalPages = 1;
  dayBookTotalElements = 0;
  daybookSortBy = 'transactionDate';
  daybookDirection = 'DESC';
  paginatedDayBookTransactions: any[] = [];

  constructor(
    private dayBookService: DayBookService,
    private toastr: ToastrService,
    private loadingService: LoadingService,
    private cdr: ChangeDetectorRef
  ) {
    // Set default date to today
    this.selectedDate = this.getTodayDate();
  }

  ngOnInit() {
    this.loadDayBookData();
  }

  ngOnDestroy() {
    // Cleanup
  }

  /**
   * Get today's date in YYYY-MM-DD format
   */
  getTodayDate(): string {
    const today = new Date();
    return today.toISOString().split('T')[0];
  }

  /**
   * Load day book transactions for current/selected date
   */
  loadDayBookData() {
    this.isLoading = true;
    this.loadingService.show('Loading Day Book...');
    
    this.dayBookService.getTransactionsByDate(
      this.selectedDate, 
      this.dayBookPage - 1, 
      this.dayBookPageSize,
      this.daybookSortBy,
      this.daybookDirection
    )
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.loadingService.hide();
        })
      )
      .subscribe({
        next: (response: any) => {
          // Response is a Page object with content, totalElements, totalPages, etc.
          this.paginatedDayBookTransactions = response.content || [];
          this.dayBookTotalPages = response.totalPages || 1;
          this.dayBookTotalElements = response.totalElements || 0;
          
          // Also fetch summary
          this.dayBookService.getTransactionsSummary(this.selectedDate)
            .subscribe({
              next: (summaryResponse: any) => {
                this.dayBookSummary = summaryResponse;
                this.dayBookTransactions = summaryResponse.transactions || [];
                this.cdr.markForCheck();
              },
              error: (error: any) => {
                console.error('Error loading day book summary:', error);
              }
            });
          
          this.cdr.markForCheck();
        },
        error: (error: any) => {
          console.error('Error loading day book data:', error);
          this.toastr.error('Failed to load day book data', 'Error');
          this.paginatedDayBookTransactions = [];
          this.dayBookSummary = null;
          this.cdr.markForCheck();
        }
      });
  }

  /**
   * Refresh data
   */
  refreshData() {
    this.loadDayBookData();
  }

  /**
   * Change date and reload
   */
  onDateChange() {
    this.loadDayBookData();
  }

  /**
   * Export Day Book to PDF
   */
  printDayBook() {
    if (this.dayBookTransactions.length === 0) {
      this.toastr.error('No data to export', 'Error');
      return;
    }

    try {
      exportDayBookReportToPDF({
        transactions: this.dayBookTransactions,
        summary: this.dayBookSummary,
        selectedDate: this.selectedDate,
        businessName: 'GAS AGENCY SYSTEM'
      });
      this.toastr.success('PDF exported successfully!', 'Success');
    } catch (error) {
      console.error('Error generating PDF:', error);
      this.toastr.error('Failed to generate PDF', 'Error');
    }
  }

  /**
   * Handle page change
   */
  onDayBookPageChange(page: number) {
    this.dayBookPage = page;
    this.loadDayBookData();
  }

  /**
   * Handle page size change
   */
  onDayBookPageSizeChange(size: number) {
    this.dayBookPageSize = size;
    this.dayBookPage = 1;
    this.loadDayBookData();
  }

  /**
   * Format amount with proper decimal places
   */
  formatAmount(amount: any): string {
    if (!amount) return '0.00';
    const num = parseFloat(amount);
    if (isNaN(num)) return '0.00';
    return num.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
}
