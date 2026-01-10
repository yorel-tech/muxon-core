'use client';

import { motion } from 'framer-motion';
import { forwardRef, useState, type ChangeEvent, type FocusEvent } from 'react';

export interface SwitchProps {
  label?: string;
  error?: string;
  helperText?: string;
  checked?: boolean;
  defaultChecked?: boolean;
  onChange?: (checked: boolean) => void;
  onFocus?: (event: FocusEvent<HTMLButtonElement>) => void;
  onBlur?: (event: FocusEvent<HTMLButtonElement>) => void;
  disabled?: boolean;
  size?: 'sm' | 'md' | 'lg';
  name?: string;
  id?: string;
  'aria-label'?: string;
  'aria-invalid'?: boolean;
  className?: string;
}

const sizeStyles: Record<string, string> = {
  sm: 'h-5 w-9',
  md: 'h-6 w-11',
  lg: 'h-7 w-13',
};

const thumbSizeStyles: Record<string, string> = {
  sm: 'h-3 w-3',
  md: 'h-4 w-4',
  lg: 'h-5 w-5',
};

export const Switch = forwardRef<HTMLButtonElement, SwitchProps>(
  ({
    label,
    error,
    helperText,
    checked,
    defaultChecked,
    onChange,
    onFocus,
    onBlur,
    disabled = false,
    size = 'md',
    name,
    id,
    'aria-label': ariaLabel,
    'aria-invalid': ariaInvalid,
    className = '',
  }: SwitchProps,
  ref,
) => {
    const [isChecked, setIsChecked] = useState(checked ?? defaultChecked ?? false);

    const handleChange = () => {
      const newValue = !isChecked;
      setIsChecked(newValue);
      onChange?.(newValue);
    };

    return (
    <div className={`${className}`}>
      <label className="flex items-center gap-3 cursor-pointer">
        <button
          ref={ref}
          type="button"
          role="switch"
          aria-checked={isChecked}
          aria-label={ariaLabel}
          aria-invalid={ariaInvalid || !!error}
          disabled={disabled}
          onClick={handleChange}
          onFocus={onFocus}
          onBlur={onBlur}
          name={name}
          id={id}
          className={`
            relative inline-flex flex-shrink-0 rounded-full p-0.5
            transition-colors duration-200 ease-in-out
            ${disabled ? 'opacity-50 cursor-not-allowed' : 'cursor-pointer'}
            ${isChecked ? 'bg-primary-500' : 'bg-gray-200'}
            ${sizeStyles[size]}
          `}
        >
          <motion.span
            className={`
              inline-block rounded-full bg-white shadow
              transform transition-transform duration-200 ease-in-out
              ${thumbSizeStyles[size]}
              ${isChecked ? 'translate-x-full' : 'translate-x-0'}
            `}
            initial={false}
            animate={{
              x: isChecked ? '100%' : '0%',
            }}
            transition={{ type: 'spring', stiffness: 500, damping: 30 }}
          />
        </button>
        {label && (
          <span className={`text-sm ${disabled ? 'text-gray-400' : 'text-gray-700'}`}>
            {label}
          </span>
        )}
      </label>
      {helperText && <p className={`mt-1.5 text-sm ${error ? 'text-error-600' : 'text-gray-500'}`}>{helperText}</p>}
      {error && !label && <p className="mt-1.5 text-sm text-error-600">{error}</p>}
    </div>
  );
});

Switch.displayName = 'Switch';
