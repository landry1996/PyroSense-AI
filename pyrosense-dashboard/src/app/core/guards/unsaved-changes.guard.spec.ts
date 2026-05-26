import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { of } from 'rxjs';
import { unsavedChangesGuard, HasUnsavedChanges } from './unsaved-changes.guard';

describe('unsavedChangesGuard', () => {
  const mockRoute = {} as ActivatedRouteSnapshot;
  const mockCurrentState = {} as RouterStateSnapshot;
  const mockNextState = {} as RouterStateSnapshot;
  let dialogSpy: jasmine.SpyObj<MatDialog>;

  beforeEach(() => {
    dialogSpy = jasmine.createSpyObj('MatDialog', ['open']);

    TestBed.configureTestingModule({
      providers: [
        { provide: MatDialog, useValue: dialogSpy },
      ],
    });
  });

  it('should return true when component has no unsaved changes', () => {
    const component: HasUnsavedChanges = { hasUnsavedChanges: () => false };
    const result = TestBed.runInInjectionContext(() =>
      unsavedChangesGuard(component, mockRoute, mockCurrentState, mockNextState));
    expect(result).toBeTrue();
  });

  it('should open dialog when component has unsaved changes and user confirms', (done) => {
    dialogSpy.open.and.returnValue({ afterClosed: () => of(true) } as any);
    const component: HasUnsavedChanges = { hasUnsavedChanges: () => true };

    const result = TestBed.runInInjectionContext(() =>
      unsavedChangesGuard(component, mockRoute, mockCurrentState, mockNextState));

    if (result === true || result === false) {
      fail('Expected observable');
    } else {
      (result as any).subscribe((val: boolean) => {
        expect(val).toBeTrue();
        expect(dialogSpy.open).toHaveBeenCalled();
        done();
      });
    }
  });

  it('should return false when user cancels dialog', (done) => {
    dialogSpy.open.and.returnValue({ afterClosed: () => of(false) } as any);
    const component: HasUnsavedChanges = { hasUnsavedChanges: () => true };

    const result = TestBed.runInInjectionContext(() =>
      unsavedChangesGuard(component, mockRoute, mockCurrentState, mockNextState));

    if (result === true || result === false) {
      fail('Expected observable');
    } else {
      (result as any).subscribe((val: boolean) => {
        expect(val).toBeFalse();
        done();
      });
    }
  });

  it('should return true when component does not implement interface', () => {
    const component = {} as HasUnsavedChanges;
    const result = TestBed.runInInjectionContext(() =>
      unsavedChangesGuard(component, mockRoute, mockCurrentState, mockNextState));
    expect(result).toBeTrue();
  });
});
