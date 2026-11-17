import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, of, switchMap } from 'rxjs';
import { ContractDetail, TravelApi, errorMessage } from './api';

@Component({
  imports: [RouterLink, DatePipe],
  template: `
    <a class="back-link" routerLink="/contracts">← Hotel contracts</a>
    @if (error()) {
      <h1>Contract details</h1><p class="notice error" role="alert">{{ error() }}</p>
    } @else if (contract(); as detail) {
      <header class="page-heading">
        <h1>{{ detail.hotelName }}</h1>
        <p>Contract #{{ detail.id }}@if (detail.city) { · {{ detail.city }} }</p>
      </header>
      <dl class="panel contract-facts">
        <div><dt>Start date</dt><dd>{{ detail.startDate | date:'MMM d, y' }}</dd></div>
        <div><dt>End date</dt><dd>{{ detail.endDate | date:'MMM d, y' }}</dd></div>
        <div><dt>Markup</dt><dd>{{ detail.markupPercentage }}%</dd></div>
      </dl>
      <div class="section-heading"><h2>Contracted room types</h2></div>
      <div class="panel table-wrap">
        <table>
          <thead><tr><th scope="col">Room type</th><th scope="col">Rate per adult per night</th><th scope="col">Rooms allocated</th><th scope="col">Adults per room</th></tr></thead>
          <tbody>
            @for (room of detail.roomTypes; track room.id) {
              <tr><td><strong>{{ room.name }}</strong></td><td>{{ detail.currency }} {{ room.ratePerAdultPerNight }}</td><td>{{ room.roomCount }}</td><td>{{ room.maxAdults }}</td></tr>
            }
          </tbody>
        </table>
      </div>
    } @else {
      <p role="status">Loading contract…</p>
    }
  `,
})
export class ContractDetailPage {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(TravelApi);
  private readonly destroyRef = inject(DestroyRef);
  readonly contract = signal<ContractDetail | null>(null);
  readonly error = signal('');

  constructor() {
    this.route.paramMap.pipe(
      switchMap(params => {
        this.error.set('');
        this.contract.set(null);
        return this.api.contract(params.get('id') ?? '').pipe(catchError(error => {
          this.error.set(errorMessage(error, 'Could not load this contract.'));
          return of(null);
        }));
      }),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(contract => this.contract.set(contract));
  }
}
