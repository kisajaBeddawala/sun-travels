import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Hotel, TravelApi, errorMessage } from './api';

@Component({
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './contract-form.html',
})
export class ContractFormPage {
  private readonly api = inject(TravelApi);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly hotels = signal<Hotel[]>([]);
  readonly loadingHotels = signal(true);
  readonly showHotelForm = signal(false);
  readonly saving = signal(false);
  readonly savingHotel = signal(false);
  readonly hotelError = signal('');
  readonly error = signal('');

  readonly hotelForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(150), Validators.pattern(/\S/)]],
    city: ['', Validators.maxLength(120)],
  });

  readonly form = this.fb.group({
    hotelId: [0, Validators.min(1)],
    startDate: ['', Validators.required],
    endDate: ['', Validators.required],
    markupPercentage: ['15', [Validators.required, Validators.pattern(/^\d{1,3}(\.\d{1,4})?$/)]],
    roomTypes: this.fb.array([this.room()]),
  });

  constructor() {
    this.loadHotels();
  }

  loadHotels() {
    this.loadingHotels.set(true);
    this.hotelError.set('');
    this.api.hotels().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: hotels => { this.hotels.set(hotels); this.loadingHotels.set(false); },
      error: error => {
        this.hotelError.set(errorMessage(error, 'Could not load hotels.'));
        this.loadingHotels.set(false);
      },
    });
  }

  private room() {
    return this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(120), Validators.pattern(/\S/)]],
      ratePerAdultPerNight: ['', [Validators.required, Validators.pattern(/^\d{1,10}(\.\d{1,2})?$/), Validators.min(0.01)]],
      roomCount: [1, [Validators.required, Validators.min(0), Validators.max(10000), Validators.pattern(/^\d+$/)]],
      maxAdults: [2, [Validators.required, Validators.min(1), Validators.max(100), Validators.pattern(/^\d+$/)]],
    });
  }

  addRoomType() {
    if (this.form.controls.roomTypes.length < 100) this.form.controls.roomTypes.push(this.room());
  }

  removeRoomType(index: number) {
    if (this.form.controls.roomTypes.length > 1) this.form.controls.roomTypes.removeAt(index);
  }

  addHotel() {
    if (this.savingHotel()) return;
    this.hotelError.set('');
    if (this.hotelForm.invalid) {
      this.hotelForm.markAllAsTouched();
      this.hotelError.set('Enter a hotel name to continue.');
      return;
    }
    this.savingHotel.set(true);
    this.api.addHotel(this.hotelForm.getRawValue()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: hotel => {
        this.hotels.update(hotels => [...hotels, hotel]);
        this.form.controls.hotelId.setValue(hotel.id);
        this.hotelForm.reset();
        this.showHotelForm.set(false);
        this.savingHotel.set(false);
      },
      error: error => {
        this.hotelError.set(errorMessage(error, 'Could not add the hotel.'));
        this.savingHotel.set(false);
      },
    });
  }

  save() {
    if (this.saving() || this.savingHotel()) return;
    this.error.set('');
    const input = this.form.getRawValue();
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set('Select a hotel and check the dates, markup, rates and room counts.');
      return;
    }
    if (input.endDate < input.startDate) {
      this.error.set('Use an end date on or after the start date.');
      return;
    }
    this.saving.set(true);
    this.api.addContract({ ...input, currency: 'USD' }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: contract => { void this.router.navigate(['/contracts', contract.id]); },
      error: error => {
        this.error.set(errorMessage(error, 'Could not save the contract.'));
        this.saving.set(false);
      },
    });
  }
}
