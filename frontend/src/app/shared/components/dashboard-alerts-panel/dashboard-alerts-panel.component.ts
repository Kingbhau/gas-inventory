import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AlertService } from '../../../services/alert.service';
import { Alert } from '../../../models/alert.model';

@Component({
  selector: 'app-dashboard-alerts-panel',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard-alerts-panel.component.html',
  styleUrl: './dashboard-alerts-panel.component.css'
})
export class DashboardAlertsPanelComponent implements OnInit {
  alerts$;
  alertCount$;

  constructor(private alertService: AlertService) {
    this.alerts$ = this.alertService.getAlerts$();
    this.alertCount$ = this.alertService.getAlertCount$();
  }

  ngOnInit(): void {
    // Component ready
  }

  dismissAlert(alertId: number): void {
    this.alertService.dismissAlert(alertId).subscribe(
      () => {
      },
      (error: any) => {
      }
    );
  }

  dismissAllAlerts(): void {
    // This would require a backend endpoint to dismiss all at once
    // For now, users can dismiss individually
    alert('Dismiss alerts individually by clicking the ✕ button on each alert');
  }

  formatTime(dateString: string): string {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMins / 60);
    const diffDays = Math.floor(diffHours / 24);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins} min${diffMins > 1 ? 's' : ''} ago`;
    if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
    return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
  }
}
