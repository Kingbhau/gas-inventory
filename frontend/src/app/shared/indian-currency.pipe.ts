import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'indianCurrency'
})
export class IndianCurrencyPipe implements PipeTransform {
  transform(value: number | string | null | undefined, symbol: string = '₹'): string {
    if (value === null || value === undefined || value === '') return '';
    const num = typeof value === 'string' ? parseFloat(value) : value;
    if (isNaN(num)) return '';
    // Format as Indian currency (lakhs/crores)
    return symbol + ' ' + num.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  }
}
