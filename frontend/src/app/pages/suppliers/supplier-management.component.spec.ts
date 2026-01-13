import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { ToastrModule } from 'ngx-toastr';
import { SupplierManagementComponent } from './supplier-management.component';

describe('SupplierManagementComponent', () => {
  let component: SupplierManagementComponent;
  let fixture: ComponentFixture<SupplierManagementComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, ToastrModule.forRoot()],
      declarations: [SupplierManagementComponent]
    }).compileComponents();
    fixture = TestBed.createComponent(SupplierManagementComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should invalidate form if required fields are missing', () => {
    component.supplierForm.patchValue({ name: '', contact: '' });
    expect(component.supplierForm.valid).toBeFalse();
  });

  it('should not allow invalid contact', () => {
    component.supplierForm.patchValue({ contact: 'abc' });
    expect(component.supplierForm.valid).toBeFalse();
  });
});
