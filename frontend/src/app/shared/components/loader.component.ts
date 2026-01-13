import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-loader',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div *ngIf="isLoading" class="loader-overlay" [class.fullscreen]="fullscreen">
      <div class="loader-container">
        <div class="spinner"></div>
        <p *ngIf="message" class="loader-message">{{ message }}</p>
      </div>
    </div>
  `,
  styles: [`
    .loader-overlay {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(255, 255, 255, 0.85);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 1000;
      border-radius: 8px;
    }

    .loader-overlay.fullscreen {
      position: fixed;
      top: 0;
      left: 0;
      right: auto;
      bottom: auto;
      width: 100vw;
      height: 100vh;
      background: rgba(0, 0, 0, 0.5);
      border-radius: 0;
    }

    .loader-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
    }

    .spinner {
      width: 50px;
      height: 50px;
      border: 4px solid #e2e8f0;
      border-top-color: #1e40af;
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .loader-message {
      font-size: 0.95rem;
      color: #475569;
      font-weight: 500;
      margin: 0;
      text-align: center;
      max-width: 300px;
    }

    @media (max-width: 768px) {
      .spinner {
        width: 40px;
        height: 40px;
        border-width: 3px;
      }

      .loader-message {
        font-size: 0.85rem;
      }
    }
  `]
})
export class LoaderComponent {
  @Input() isLoading = false;
  @Input() message = '';
  @Input() fullscreen = false;
}
