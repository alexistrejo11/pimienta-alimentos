import type { ParsedApiError } from '../http/parse-api-error';

/** User-facing Spanish message for auth forms (login / register). Never exposes trace ids. */
export function authUserMessage(parsed: ParsedApiError, context: 'login' | 'register' = 'login'): string {
  const code = parsed.errorCode;
  const status = parsed.httpStatus;

  if (status === 0) {
    return 'No se pudo conectar con el servidor. Revise su conexión.';
  }
  if (status === 429) {
    return 'Demasiados intentos. Espere un momento e intente de nuevo.';
  }

  if (
    code === 'AUTHENTICATION_FAILED' ||
    code === 'UNAUTHORIZED' ||
    (context === 'login' && status === 401)
  ) {
    return 'Correo o contraseña incorrectos. Verifique e intente de nuevo.';
  }

  if (code === 'USER_BANNED') {
    return 'Su cuenta está suspendida. Contacte a TI para más información.';
  }

  if (code === 'EMAIL_ALREADY_REGISTERED' || code === 'CONFLICT') {
    return parsed.message?.trim() || 'Ya existe una cuenta con esos datos.';
  }

  const message = parsed.message?.trim();
  if (message && !looksTechnical(message)) {
    return message;
  }

  return context === 'register'
    ? 'No se pudo completar el registro. Intente de nuevo.'
    : 'No se pudo iniciar sesión. Intente de nuevo.';
}

function looksTechnical(message: string): boolean {
  return (
    /^(Http failure|Unknown Error|Error de red|Internal Server|Exception\b)/i.test(message) ||
    /^[A-Z][A-Z0-9_]{4,}$/.test(message)
  );
}
