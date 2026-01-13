import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToastrModule } from 'ngx-toastr';
import { ReportsComponent } from './reports.component';

describe('ReportsComponent', () => {
  let component: ReportsComponent;
  let fixture: ComponentFixture<ReportsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToastrModule.forRoot()],
      declarations: [ReportsComponent]
    }).compileComponents();
    fixture = TestBed.createComponent(ReportsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should return 0 for totalSalesAmount if no data', () => {
    component.salesData = [];
    expect(component.totalSalesAmount).toBe(0);
  });

  it('should return 0 for avgSaleValue if no data', () => {
    component.salesData = [];
    expect(component.avgSaleValue).toBe(0);
  });

  it('should return N/A for topCustomer if no data', () => {
    component.salesData = [];
    expect(component.topCustomer).toBe('N/A');
  });
});
