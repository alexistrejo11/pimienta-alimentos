import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';

import { EmployeeService } from '../../../../core/employees/employee.service';
import { markFormPristine } from '../../../../core/forms/mark-form-pristine';
import {
  fieldMessage,
  parseApiError,
  type ParsedApiError,
} from '../../../../core/http/parse-api-error';
import type {
  ContractType,
  EmployeeOnboardingPhase,
  WorkShift,
} from '../../../../core/model/employee/employee.enums';
import type {
  EmployeeResponse,
  RegisterEmployeeRequest,
  UpdateEmployeeRequest,
} from '../../../../core/model/employee/employee.dto';
import { PageHeaderComponent } from '../../../../shared/ui/page-header/page-header';

const CONTRACT_TYPES: ContractType[] = [
  'INDEFINITE',
  'FIXED_TERM',
  'PROJECT_BASED',
  'TEMPORARY',
  'FREELANCE',
  'UNDEFINED',
];

const WORK_SHIFTS: WorkShift[] = [
  'MORNING',
  'AFTERNOON',
  'NIGHT',
  'MIXED',
  'REMOTE',
  'UNDEFINED',
];

const ONBOARDING_PHASES: EmployeeOnboardingPhase[] = ['DRAFT', 'PENDING_CONTRACT'];

const MAX_PHOTO_BYTES = 2 * 1024 * 1024;
const ALLOWED_PHOTO_TYPES = new Set(['image/jpeg', 'image/png']);

@Component({
  selector: 'app-empleado-form-page',
  imports: [ReactiveFormsModule, RouterLink, PageHeaderComponent],
  templateUrl: './empleado-form-page.html',
})
export class EmpleadoFormPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly service = inject(EmployeeService);

  readonly loading = signal(false);
  readonly loadingExisting = signal(false);
  readonly apiError = signal<ParsedApiError | null>(null);
  readonly photoError = signal<string | null>(null);
  readonly employeeId = signal<number | null>(null);
  readonly photoFile = signal<File | null>(null);
  readonly existingPhotoUrl = signal<string | null>(null);
  readonly photoPreviewUrl = signal<string | null>(null);

  readonly contractTypes = CONTRACT_TYPES;
  readonly workShifts = WORK_SHIFTS;
  readonly onboardingPhases = ONBOARDING_PHASES;

  readonly contractTypeLabel: Record<ContractType, string> = {
    INDEFINITE: 'Indefinido',
    FIXED_TERM: 'Plazo fijo',
    PROJECT_BASED: 'Por proyecto',
    TEMPORARY: 'Temporal',
    FREELANCE: 'Freelance',
    UNDEFINED: 'Sin definir',
  };

  readonly workShiftLabel: Record<WorkShift, string> = {
    MORNING: 'Matutino',
    AFTERNOON: 'Vespertino',
    NIGHT: 'Nocturno',
    MIXED: 'Mixto',
    REMOTE: 'Remoto',
    UNDEFINED: 'Sin definir',
  };

  readonly onboardingPhaseLabel: Record<EmployeeOnboardingPhase, string> = {
    DRAFT: 'Borrador',
    PENDING_CONTRACT: 'Pendiente de contrato',
  };

  readonly form = this.fb.group({
    firstName: ['', [Validators.required, Validators.maxLength(100)]],
    lastName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.email, Validators.maxLength(320)]],
    phone: ['', [Validators.maxLength(40)]],
    address: ['', [Validators.maxLength(500)]],
    curp: ['', [Validators.maxLength(18)]],
    rfc: ['', [Validators.maxLength(13)]],
    nss: ['', [Validators.maxLength(11)]],
    clabe: ['', [Validators.maxLength(18)]],
    employeeNumber: ['', [Validators.maxLength(32)]],
    position: ['', [Validators.maxLength(120)]],
    department: ['', [Validators.maxLength(120)]],
    contractType: this.fb.control<ContractType | ''>(''),
    workShift: this.fb.control<WorkShift | ''>(''),
    salaryPerWeek: [''],
    birthDate: [''],
    onboardingPhase: this.fb.control<EmployeeOnboardingPhase | ''>('DRAFT'),
    bonuses: [''],
    foodVouchers: [''],
    integrationFactor: [''],
  });

  ngOnInit(): void {
    const isEditRoute = this.route.snapshot.url.some((u) => u.path === 'editar');
    const idRaw = isEditRoute ? this.route.snapshot.paramMap.get('id') : null;
    if (idRaw) {
      const id = Number(idRaw);
      if (!Number.isFinite(id) || id <= 0) {
        void this.router.navigateByUrl('/app/empleados');
        return;
      }
      this.employeeId.set(id);
      this.loadEmployee(id);
    }
  }

  get isEdit(): boolean {
    return this.employeeId() != null;
  }

  displayPhotoUrl(): string | null {
    return this.photoPreviewUrl() ?? this.existingPhotoUrl();
  }

  onPhotoSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.photoError.set(null);
    this.photoFile.set(null);
    this.photoPreviewUrl.set(null);

    if (!file) return;

    if (!ALLOWED_PHOTO_TYPES.has(file.type)) {
      this.photoError.set('Solo se permiten imágenes JPEG o PNG.');
      input.value = '';
      return;
    }
    if (file.size > MAX_PHOTO_BYTES) {
      this.photoError.set('La imagen no debe superar 2 MB.');
      input.value = '';
      return;
    }

    this.photoFile.set(file);
    this.photoPreviewUrl.set(URL.createObjectURL(file));
  }

  submit(): void {
    this.apiError.set(null);
    if (this.photoError()) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const id = this.employeeId();
    this.loading.set(true);
    const photo = this.photoFile() ?? undefined;

    if (id == null) {
      this.service
        .register(this.buildRegisterBody(), photo)
        .pipe(finalize(() => this.loading.set(false)))
        .subscribe({
          next: (created) => {
            markFormPristine(this.form);
            void this.router.navigateByUrl(`/app/empleados/${created.id}`);
          },
          error: (err: unknown) => this.apiError.set(parseApiError(err)),
        });
    } else {
      this.service
        .update(id, this.buildUpdateBody(), photo)
        .pipe(finalize(() => this.loading.set(false)))
        .subscribe({
          next: () => {
            markFormPristine(this.form);
            void this.router.navigateByUrl(`/app/empleados/${id}`);
          },
          error: (err: unknown) => this.apiError.set(parseApiError(err)),
        });
    }
  }

  apiFieldMessage(field: string): string | undefined {
    const p = this.apiError();
    if (!p) return undefined;
    return fieldMessage(p, field);
  }

  private loadEmployee(id: number): void {
    this.loadingExisting.set(true);
    this.apiError.set(null);
    this.service.getById(id).subscribe({
      next: (e: EmployeeResponse) => {
        this.patchFromEmployee(e);
        this.existingPhotoUrl.set(e.photoUrl || null);
        this.loadingExisting.set(false);
      },
      error: (err: unknown) => {
        this.apiError.set(parseApiError(err));
        this.loadingExisting.set(false);
      },
    });
  }

  private patchFromEmployee(e: EmployeeResponse): void {
    this.form.patchValue({
      firstName: e.firstName ?? '',
      lastName: e.lastName ?? '',
      email: e.email ?? '',
      phone: e.phone ?? '',
      address: e.address ?? '',
      curp: e.curp ?? '',
      rfc: e.rfc ?? '',
      nss: e.nss ?? '',
      clabe: e.clabe ?? '',
      position: e.position ?? '',
      department: e.department ?? '',
      contractType: e.contractType ?? '',
      workShift: e.workShift ?? '',
      salaryPerWeek: e.salaryPerWeek != null ? String(e.salaryPerWeek) : '',
      bonuses: e.bonuses != null ? String(e.bonuses) : '',
      foodVouchers: e.foodVouchers != null ? String(e.foodVouchers) : '',
      integrationFactor: e.integrationFactor != null ? String(e.integrationFactor) : '',
      birthDate: e.birthDate ?? '',
    });
    markFormPristine(this.form);
  }

  private optionalString(value: string | null | undefined): string | null {
    if (value == null) return null;
    const t = value.trim();
    return t.length === 0 ? null : t;
  }

  private optionalNumber(value: string | null | undefined): number | null {
    if (value == null || String(value).trim() === '') return null;
    const n = Number(value);
    return Number.isFinite(n) ? n : null;
  }

  private buildRegisterBody(): RegisterEmployeeRequest {
    const v = this.form.getRawValue();
    return {
      firstName: (v.firstName ?? '').trim(),
      lastName: (v.lastName ?? '').trim(),
      email: this.optionalString(v.email),
      phone: this.optionalString(v.phone),
      address: this.optionalString(v.address),
      curp: this.optionalString(v.curp),
      rfc: this.optionalString(v.rfc),
      nss: this.optionalString(v.nss),
      clabe: this.optionalString(v.clabe),
      employeeNumber: this.optionalString(v.employeeNumber),
      position: this.optionalString(v.position),
      department: this.optionalString(v.department),
      contractType: v.contractType || null,
      workShift: v.workShift || null,
      salaryPerWeek: this.optionalNumber(v.salaryPerWeek),
      birthDate: this.optionalString(v.birthDate),
      onboardingPhase: v.onboardingPhase || null,
    };
  }

  private buildUpdateBody(): UpdateEmployeeRequest {
    const v = this.form.getRawValue();
    return {
      firstName: (v.firstName ?? '').trim(),
      lastName: (v.lastName ?? '').trim(),
      email: this.optionalString(v.email),
      phone: this.optionalString(v.phone),
      address: this.optionalString(v.address),
      curp: this.optionalString(v.curp),
      rfc: this.optionalString(v.rfc),
      nss: this.optionalString(v.nss),
      clabe: this.optionalString(v.clabe),
      position: this.optionalString(v.position),
      department: this.optionalString(v.department),
      contractType: v.contractType || null,
      workShift: v.workShift || null,
      salaryPerWeek: this.optionalNumber(v.salaryPerWeek),
      bonuses: this.optionalNumber(v.bonuses),
      foodVouchers: this.optionalNumber(v.foodVouchers),
      integrationFactor: this.optionalNumber(v.integrationFactor),
    };
  }
}
