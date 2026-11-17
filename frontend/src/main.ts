import { provideHttpClient } from '@angular/common/http';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter, Routes } from '@angular/router';
import { App } from './app/app';

const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'search' },
  { path: 'search', title: 'Room search · Sun Travels', loadComponent: () => import('./app/search').then(m => m.SearchPage) },
  { path: 'contracts', title: 'Hotel contracts · Sun Travels', loadComponent: () => import('./app/contracts').then(m => m.ContractsPage) },
  { path: 'contracts/new', title: 'New contract · Sun Travels', loadComponent: () => import('./app/contract-form').then(m => m.ContractFormPage) },
  { path: 'contracts/:id', title: 'Contract details · Sun Travels', loadComponent: () => import('./app/contract-detail').then(m => m.ContractDetailPage) },
  { path: '**', redirectTo: 'search' },
];

bootstrapApplication(App, { providers: [provideRouter(routes), provideHttpClient()] }).catch(console.error);
