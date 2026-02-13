import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AlertSettingsService } from 'src/app/services/alert-settings.service';
import { AlertConfig } from 'src/app/models/alert-config.model';

@Component({
  selector: 'app-alert-settings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './alert-settings.component.html',
  styleUrl: './alert-settings.component.css'
})
export class AlertSettingsComponent implements OnInit {
  settingsForm!: FormGroup;
  loading = false;
  saving = false;
  successMessage = '';
  errorMessage = '';

  constructor(
    private fb: FormBuilder,
    private alertSettingsService: AlertSettingsService
  ) {
    this.initializeForm();
  }

  ngOnInit(): void {
    this.loadSettings();
  }

  private initializeForm(): void {
    this.settingsForm = this.fb.group({
      lowStockEnabled: [true],
      filledThreshold: [50, [Validators.required, Validators.min(0)]],
      emptyThreshold: [50, [Validators.required, Validators.min(0)]],
      pendingReturnEnabled: [true],
      pendingReturnThreshold: [10, [Validators.required, Validators.min(0)]]
    });
  }

  private loadSettings(): void {
    this.loading = true;
    this.alertSettingsService.getAlertConfigurations().subscribe(
      (response: AlertConfig[]) => {
        if (response) {
          this.mapConfigsToForm(response);
        }
        this.loading = false;
      },
      (error: unknown) => {
        this.errorMessage = 'Failed to load alert settings';
        this.loading = false;
      }
    );
  }

  private mapConfigsToForm(configs: AlertConfig[]): void {
    configs.forEach(config => {
      if (config.alertType === 'LOW_STOCK_WAREHOUSE') {
        this.settingsForm.patchValue({
          lowStockEnabled: config.enabled,
          filledThreshold: config.filledCylinderThreshold || 50,
          emptyThreshold: config.emptyCylinderThreshold || 50
        });
      }
      if (config.alertType === 'PENDING_RETURN_CYLINDERS') {
        this.settingsForm.patchValue({
          pendingReturnEnabled: config.enabled,
          pendingReturnThreshold: config.pendingReturnThreshold || 10
        });
      }
    });
    this.settingsForm.markAsPristine();
  }

  saveSettings(): void {
    if (!this.settingsForm.valid) {
      this.errorMessage = 'Please fix form errors';
      return;
    }

    this.saving = true;
    this.successMessage = '';
    this.errorMessage = '';

    const formValue = this.settingsForm.value;

    // Update LOW_STOCK
    this.alertSettingsService.updateAlertConfig('LOW_STOCK_WAREHOUSE', {
      enabled: formValue.lowStockEnabled,
      filledCylinderThreshold: formValue.filledThreshold,
      emptyCylinderThreshold: formValue.emptyThreshold
    }).subscribe(
      () => {
        // Update PENDING_RETURN - Must include all fields: enabled, pendingReturnThreshold
        this.alertSettingsService.updateAlertConfig('PENDING_RETURN_CYLINDERS', {
          enabled: formValue.pendingReturnEnabled,
          pendingReturnThreshold: formValue.pendingReturnThreshold
        }).subscribe(
          () => {
            this.saving = false;
            this.successMessage = 'Alert settings saved successfully!';
            this.settingsForm.markAsPristine();
            setTimeout(() => this.successMessage = '', 3000);
          },
          (error: unknown) => {
            this.saving = false;
            this.errorMessage = 'Failed to save pending returns alert settings';
          }
        );
      },
      (error: unknown) => {
        this.saving = false;
        this.errorMessage = 'Failed to save low stock alert settings';
      }
    );
  }

  resetForm(): void {
    this.settingsForm.reset();
    this.loadSettings();
  }
}
