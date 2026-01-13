import { NgModule } from '@angular/core';
import { IndianCurrencyPipe } from './indian-currency.pipe';
import { IndianCurrencyFormatDirective } from './indian-currency-format.directive';
import { CommonModule } from '@angular/common';

@NgModule({
  declarations: [IndianCurrencyPipe, IndianCurrencyFormatDirective],
  imports: [CommonModule],
  exports: [IndianCurrencyPipe, IndianCurrencyFormatDirective]
})
export class SharedModule {}
