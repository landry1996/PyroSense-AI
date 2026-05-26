import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatusChipComponent } from './status-chip.component';

describe('StatusChipComponent', () => {
  let component: StatusChipComponent;
  let fixture: ComponentFixture<StatusChipComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatusChipComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(StatusChipComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display status text', () => {
    component.status = 'OPEN';
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.chip').textContent.trim()).toBe('OPEN');
  });

  it('should display custom label when provided', () => {
    component.status = 'IN_PROGRESS';
    component.label = 'En cours';
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.chip').textContent.trim()).toBe('En cours');
  });

  it('should apply status class in lowercase', () => {
    component.status = 'ACTIVE';
    fixture.detectChanges();

    const chip = fixture.nativeElement.querySelector('.chip');
    expect(chip.classList).toContain('active');
  });
});
