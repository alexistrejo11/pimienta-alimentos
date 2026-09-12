import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { fieldMessage, parseApiError, type ParsedApiError } from '../../../../core/http/parse-api-error';
import type { TaskPriority } from '../../../../core/model/task/task.enums';
import type { ChecklistLineRequest, TaskRequest } from '../../../../core/model/task/task.dto';
import { TaskService } from '../../../../core/tasks/task.service';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';
import { HeadquarterSelectComponent } from '../../../../shared/ui/headquarter-select/headquarter-select';
import { EmployeeSelectComponent } from '../../../../shared/ui/employee-select/employee-select';
import { OpportunitySelectComponent } from '../../../../shared/ui/opportunity-select/opportunity-select';
import { ProjectSelectComponent } from '../../../../shared/ui/project-select/project-select';
import { ApiFormErrorComponent } from '../../../../shared/ui/api-form-error/api-form-error';

const PRIORITIES: TaskPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT', 'UNDEFINED'];

@Component({
  selector: 'app-tarea-form-page',
  
  imports: [
    ReactiveFormsModule,
    RouterLink,
    PageHeaderComponent,
    HeadquarterSelectComponent,
    EmployeeSelectComponent,
    OpportunitySelectComponent,
    ProjectSelectComponent,
    ApiFormErrorComponent,
  ],
  templateUrl: './tarea-form-page.html',
})
export class TareaFormPageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly tasks = inject(TaskService);

  readonly saving = signal(false);
  readonly apiError = signal<ParsedApiError | null>(null);
  readonly priorities = PRIORITIES;
  readonly priorityLabel: Record<TaskPriority, string> = {
    LOW: 'Baja',
    MEDIUM: 'Media',
    HIGH: 'Alta',
    URGENT: 'Urgente',
    UNDEFINED: 'Sin definir',
  };

  readonly form = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(500)]],
    description: [''],
    priority: this.fb.nonNullable.control<TaskPriority>('MEDIUM'),
    dueDate: [''],
    headquarterId: this.fb.control<number | null>(null),
    projectId: this.fb.control<number | null>(null),
    opportunityId: this.fb.control<number | null>(null),
    createdById: this.fb.control<number | null>(null),
    checklistText: [''],
  });

  private asText(value: unknown): string {
    if (value == null) return '';
    return String(value).trim();
  }

  submit(): void {
    this.apiError.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.tasks
      .create(this.buildRequest())
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (created) => void this.router.navigateByUrl(`/app/tareas/${created.id}`),
        error: (err: unknown) => this.apiError.set(parseApiError(err)),
      });
  }

  private parseChecklist(raw: string): ChecklistLineRequest[] {
    return raw
      .split('\n')
      .map((line) => line.trim())
      .filter((line) => line.length > 0)
      .map((description, idx) => ({ description, displayOrder: idx }));
  }

  private buildRequest(): TaskRequest {
    const v = this.form.getRawValue();
    const body: TaskRequest = {
      title: this.asText(v.title),
      priority: v.priority,
    };

    const description = this.asText(v.description);
    if (description) body.description = description;

    const dueDate = this.asText(v.dueDate);
    if (dueDate) {
      body.dueDate = new Date(dueDate).toISOString();
    }

    if (v.headquarterId != null && v.headquarterId > 0) {
      body.headquarterId = v.headquarterId;
    }
    if (v.projectId != null && v.projectId > 0) {
      body.projectId = v.projectId;
    }
    if (v.opportunityId != null && v.opportunityId > 0) {
      body.opportunityId = v.opportunityId;
    }
    if (v.createdById != null && v.createdById > 0) {
      body.createdById = v.createdById;
    }

    const checklist = this.parseChecklist(this.asText(v.checklistText));
    if (checklist.length > 0) body.checklist = checklist;

    return body;
  }

  onHeadquarterChange(value: number | number[] | null): void {
    const id = Array.isArray(value) ? value[0] ?? null : value;
    this.form.controls.headquarterId.setValue(id);
  }

  apiFieldMessage(field: string): string | undefined {
    const p = this.apiError();
    if (!p) return undefined;
    return fieldMessage(p, field);
  }
}
