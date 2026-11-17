import { DatePipe } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { SearchInput, SearchResult, TravelApi, UnavailabilityReason, errorMessage } from './api';

@Component({
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './search.html',
})
export class SearchPage {
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly api = inject(TravelApi);
  private readonly destroyRef = inject(DestroyRef);
  readonly busy = signal(false);
  readonly error = signal('');
  readonly result = signal<SearchResult | null>(null);
  readonly quotedRequest = signal<SearchInput | null>(null);
  readonly quoteOccupancy = computed(() => this.quotedRequest()?.rooms.map(room => room.adults).join(', ') ?? '');
  readonly availableCount = computed(() => this.result()?.results.filter(o => o.availability === 'AVAILABLE').length ?? 0);
  readonly reasonText: Record<UnavailabilityReason, string> = {
    INSUFFICIENT_ROOMS: 'Not enough rooms',
    OCCUPANCY_EXCEEDED: 'Adult limit exceeded',
  };
  readonly form = this.fb.group({
    checkInDate: ['2027-06-10', Validators.required],
    numberOfNights: [3, [Validators.required, Validators.min(1), Validators.max(365), Validators.pattern(/^\d+$/)]],
    rooms: this.fb.array([this.room(2), this.room(1)]),
  });

  private room(adults: number) {
    return this.fb.group({
      adults: [adults, [Validators.required, Validators.min(1), Validators.max(100), Validators.pattern(/^\d+$/)]],
    });
  }

  addRoom() {
    if (this.form.controls.rooms.length < 100) this.form.controls.rooms.push(this.room(1));
  }

  removeRoom(index: number) {
    if (this.form.controls.rooms.length > 1) this.form.controls.rooms.removeAt(index);
  }

  search() {
    if (this.busy()) return;
    this.error.set('');
    this.result.set(null);
    this.quotedRequest.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Enter a check-in date, 1 to 365 nights, and 1 to 100 adults in each requested room.');
      return;
    }
    this.busy.set(true);
    const input = this.form.getRawValue();
    this.api.search(input).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: result => {
        this.quotedRequest.set(input);
        this.result.set(result);
        this.busy.set(false);
      },
      error: error => {
        this.error.set(errorMessage(error, 'Room search failed. Check that the backend is running, then try again.'));
        this.busy.set(false);
      },
    });
  }
}
