import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { TaskService } from '../../../core/tasks/task.service';
import { parseApiError, type ParsedApiError } from '../../../core/http/parse-api-error';
import type { TaskListItemResponse } from '../../../core/model/task/task.dto';
import { PageHeaderComponent } from '../../../shared/ui/page-header/page-header';
import { DataStateComponent } from '../../../shared/ui/data-state/data-state';
import { TareaRowComponent } from './components/tarea-row/tarea-row';

/** Página principal del módulo de tareas. */
@Component({
  selector: 'app-tareas-page',
  
  imports: [PageHeaderComponent, DataStateComponent, TareaRowComponent, RouterLink],
  templateUrl: './tareas-page.html',
})
export class TareasPageComponent implements OnInit {
  private readonly service = inject(TaskService);

  readonly loading = signal(true);
  readonly error = signal<ParsedApiError | null>(null);
  readonly tareas = signal<TaskListItemResponse[]>([]);

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.error.set(null);
    this.loading.set(true);
    this.service
      .list()
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (page) => this.tareas.set(page.items),
        error: (err: unknown) => this.error.set(parseApiError(err)),
      });
  }
}
