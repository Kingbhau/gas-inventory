import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ToastrModule } from 'ngx-toastr';
import { EmptyReturnComponent } from './empty-return.component';

describe('EmptyReturnComponent', () => {
  let component: EmptyReturnComponent;
  let fixture: ComponentFixture<EmptyReturnComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, ToastrModule.forRoot()],
      declarations: [EmptyReturnComponent]
    }).compileComponents();
    fixture = TestBed.createComponent(EmptyReturnComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should invalidate form if required fields are missing', () => {
    component.emptyReturnForm.patchValue({ customerId: '', variantId: '', emptyIn: '', transactionDate: '' });
    expect(component.emptyReturnForm.valid).toBeFalse();
  });

  it('should not allow emptyIn < 1', () => {
    component.emptyReturnForm.patchValue({ emptyIn: 0 });
    expect(component.emptyReturnForm.valid).toBeFalse();
  });
});
