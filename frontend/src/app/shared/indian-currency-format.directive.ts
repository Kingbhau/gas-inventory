import { Directive, HostListener, ElementRef, OnInit, OnDestroy } from '@angular/core';
import { NgControl } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Directive({
  selector: '[indianCurrencyFormat]'
})
export class IndianCurrencyFormatDirective implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  constructor(private el: ElementRef, private control: NgControl) {}

  ngOnInit() {
    // Listen for form control value changes and format them
    if (this.control.control) {
      this.control.control.valueChanges
        .pipe(takeUntil(this.destroy$))
        .subscribe(() => {
          setTimeout(() => {
            this.formatDisplay();
          }, 0);
        });
    }
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private formatDisplay() {
    const input = this.el.nativeElement as HTMLInputElement;
    let value = input.value.replace(/,/g, '');
    
    if (value && value !== '') {
      const sanitizedValue = value.replace(/[^0-9.]/g, '');
      if (sanitizedValue !== '' && !isNaN(Number(sanitizedValue))) {
        input.value = this.formatIndianCurrency(sanitizedValue);
      }
    }
  }

  @HostListener('input', ['$event'])
  onInput(event: Event) {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/,/g, '');
    
    // Allow only digits and decimal point
    const sanitizedValue = value.replace(/[^0-9.]/g, '');
    
    // Prevent multiple decimal points
    let finalValue = sanitizedValue;
    const decimalCount = (sanitizedValue.match(/\./g) || []).length;
    if (decimalCount > 1) {
      finalValue = sanitizedValue.substring(0, sanitizedValue.lastIndexOf('.'));
    }
    
    // Update control value immediately without emitting change event
    this.control.control?.setValue(finalValue, { emitEvent: false });
    
    // Update the display with formatted value
    input.value = finalValue === '' ? '' : this.formatIndianCurrency(finalValue);
  }

  @HostListener('blur', ['$event'])
  onBlur(event: Event) {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/,/g, '');
    if (value !== '' && !isNaN(Number(value))) {
      input.value = this.formatIndianCurrency(value);
    }
  }

  private formatIndianCurrency(value: string): string {
    // Format as Indian currency (e.g., 1234567 => 12,34,567)
    let x = value.split('.');
    let integerPart = x[0];
    let lastThree = integerPart.substring(integerPart.length - 3);
    let otherNumbers = integerPart.substring(0, integerPart.length - 3);
    if (otherNumbers !== '') {
      lastThree = ',' + lastThree;
    }
    let formatted = otherNumbers.replace(/\B(?=(\d{2})+(?!\d))/g, ',') + lastThree;
    if (x.length > 1) {
      formatted += '.' + x[1];
    }
    return formatted;
  }
}
