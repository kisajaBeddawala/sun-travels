import { DatePipe } from '@angular/common';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { ContractSummary, TravelApi, errorMessage } from './api';

@Component({
  imports: [RouterLink, DatePipe],
  template: `
    <header class="page-heading page-heading-row">
      <div><h1>Hotel contracts</h1><p>View contracted rates, dates and room allocations.</p></div>
      <a routerLink="/contracts/new" class="button primary">+ New contract</a>
    </header>
    @if (error()) {
      <p class="notice error" role="alert">{{ error() }}</p>
      <button type="button" class="button secondary" (click)="load()">Retry</button>
    } @else if (loading()) {
      <p role="status">Loading contracts…</p>
    } @else if (contracts().length) {
      <div class="panel table-wrap">
        <table>
          <thead><tr><th scope="col">Hotel</th><th scope="col">Contract dates</th><th scope="col">Room types</th><th scope="col">Markup</th><th scope="col">Details</th></tr></thead>
          <tbody>
            @for (contract of contracts(); track contract.id) {
              <tr>
                <td><strong>{{ contract.hotelName }}</strong>@if (contract.city) { <small>{{ contract.city }}</small> }</td>
                <td>{{ contract.startDate | date:'MMM d, y' }} to {{ contract.endDate | date:'MMM d, y' }}</td>
                <td>{{ contract.roomTypeCount }}</td>
                <td>{{ contract.markupPercentage }}%</td>
                <td><a [routerLink]="['/contracts', contract.id]" [attr.aria-label]="'View contract ' + contract.id + ' for ' + contract.hotelName">View #{{ contract.id }}</a></td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    } @else {
      <div class="panel empty"><strong>No contracts yet.</strong><p>Create a contract to make its rooms searchable.</p></div>
    }
  `,
})
export class ContractsPage {
  private readonly api = inject(TravelApi);
  private readonly destroyRef = inject(DestroyRef);
  readonly contracts = signal<ContractSummary[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');

  constructor() { this.load(); }

  load() {
    this.loading.set(true);
    this.error.set('');
    this.api.contracts().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: contracts => { this.contracts.set(contracts); this.loading.set(false); },
      error: error => {
        this.error.set(errorMessage(error, 'Could not load hotel contracts.'));
        this.loading.set(false);
      },
    });
  }
}
