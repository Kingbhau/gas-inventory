import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ToastrModule } from 'ngx-toastr';
import { CustomerManagementComponent } from './customer-management.component';

describe('CustomerManagementComponent', () => {
  let component: CustomerManagementComponent;
  let fixture: ComponentFixture<CustomerManagementComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, ToastrModule.forRoot()],
      declarations: [CustomerManagementComponent]
    }).compileComponents();
    fixture = TestBed.createComponent(CustomerManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should invalidate form if required fields are missing', () => {
    component.customerForm.patchValue({ name: '', mobile: '', address: '' });
    expect(component.customerForm.valid).toBeFalse();
  });

  it('should not allow invalid mobile number', () => {
    component.customerForm.patchValue({ mobile: '12345' });
    expect(component.customerForm.valid).toBeFalse();
  });
});
