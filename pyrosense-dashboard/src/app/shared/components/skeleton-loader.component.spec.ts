import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SkeletonLoaderComponent } from './skeleton-loader.component';

describe('SkeletonLoaderComponent', () => {
  let component: SkeletonLoaderComponent;
  let fixture: ComponentFixture<SkeletonLoaderComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SkeletonLoaderComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(SkeletonLoaderComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have role=status for accessibility', () => {
    const el = fixture.nativeElement.querySelector('[role="status"]');
    expect(el).toBeTruthy();
  });

  it('should have aria-label for accessibility', () => {
    const el = fixture.nativeElement.querySelector('[aria-label="Chargement en cours"]');
    expect(el).toBeTruthy();
  });

  it('default type should render a line', () => {
    const lines = fixture.nativeElement.querySelectorAll('.skeleton-line');
    expect(lines.length).toBeGreaterThan(0);
  });

  it('type=stat should render circle', () => {
    component.type = 'stat';
    fixture.detectChanges();
    const circle = fixture.nativeElement.querySelector('.skeleton-circle');
    expect(circle).toBeTruthy();
  });

  it('type=table should render rows and cells', () => {
    component.type = 'table';
    component.count = 3;
    component.columns = 4;
    fixture.detectChanges();
    const rows = fixture.nativeElement.querySelectorAll('.skeleton-row');
    expect(rows.length).toBe(3);
    const cells = fixture.nativeElement.querySelectorAll('.skeleton-cell');
    expect(cells.length).toBe(12);
  });

  it('type=card should render skeleton-card', () => {
    component.type = 'card';
    fixture.detectChanges();
    const card = fixture.nativeElement.querySelector('.skeleton-card');
    expect(card).toBeTruthy();
  });
});
