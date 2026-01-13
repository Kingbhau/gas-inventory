import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ToastrModule } from 'ngx-toastr';
import { SaleEntryComponent } from './sale-entry.component';

describe('SaleEntryComponent', () => {
  let component: SaleEntryComponent;
  let fixture: ComponentFixture<SaleEntryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, ToastrModule.forRoot()],
      declarations: [SaleEntryComponent]
    }).compileComponents();
    fixture = TestBed.createComponent(SaleEntryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should invalidate form if required fields are missing', () => {
    component.saleForm.patchValue({ customerId: '', variantId: '', filledIssuedQty: '', basePrice: '', discount: '' });
    expect(component.saleForm.valid).toBeFalse();
  });

  it('should not allow negative discount or filledIssuedQty', () => {
    component.saleForm.patchValue({ filledIssuedQty: -1, discount: -5 });
    component.calculateTotal();
    expect(component.baseAmount).toBe(0);
    expect(component.totalAmount).toBe(0);
  });

  it('should calculate total correctly', () => {
    component.saleForm.patchValue({ filledIssuedQty: 2, basePrice: 100, discount: 10 });
    component.calculateTotal();
    expect(component.baseAmount).toBe(200);
    expect(component.totalAmount).toBe(190);
  });
});
