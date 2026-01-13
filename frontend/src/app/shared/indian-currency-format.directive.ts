import { Directive, HostListener, ElementRef } from '@angular/core';
import { NgControl } from '@angular/forms';

@Directive({
  selector: '[indianCurrencyFormat]'
})
export class IndianCurrencyFormatDirective {
  constructor(private el: ElementRef, private control: NgControl) {}

  @HostListener('input', ['$event'])
  onInput(event: Event) {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/,/g, '');
    if (!isNaN(Number(value)) && value !== '') {
      // Update the form control value without commas
      this.control.control?.setValue(value.replace(/,/g, ''));
      // Format after Angular updates
      setTimeout(() => {
        input.value = this.formatIndianCurrency(value);
      });
    }
  }

  @HostListener('blur', ['$event'])
  onBlur(event: Event) {
    const input = event.target as HTMLInputElement;
    let value = input.value.replace(/,/g, '');
    if (!isNaN(Number(value)) && value !== '') {
      setTimeout(() => {
        input.value = this.formatIndianCurrency(value);
      });
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
