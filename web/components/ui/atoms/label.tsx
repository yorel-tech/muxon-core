'use client';

import { forwardRef, type ReactNode, type FocusEvent } from 'react';

export interface LabelProps {
  children: ReactNode;
  htmlFor?: string;
  required?: boolean;
  disabled?: boolean;
  error?: boolean;
  className?: string;
}

export const Label = forwardRef<HTMLLabelElement, LabelProps>(
  ({ children, htmlFor, required = false, disabled = false, error = false, className = '' }: LabelProps, ref) => {
    return (
      <label
        ref={ref}
        htmlFor={htmlFor}
        className={`block text-sm font-medium ${disabled ? 'text-gray-400' : error ? 'text-error-600' : 'text-gray-700'} ${className}`}
      >
        {children}
        {required && <span className="text-error-500 ml-1" aria-label="required">*</span>}
      </label>
    );
  });

Label.displayName = 'Label';
