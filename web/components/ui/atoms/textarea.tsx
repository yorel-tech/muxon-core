'use client';

import { motion } from 'framer-motion';
import { forwardRef, useState, type ReactNode, type ChangeEvent, type FocusEvent } from 'react';
import { AlertCircle } from 'lucide-react';

export interface TextareaProps {
  label?: string;
  error?: string;
  helperText?: string;
  fullWidth?: boolean;
  size?: 'sm' | 'md' | 'lg';
  disabled?: boolean;
  placeholder?: string;
  value?: string;
  defaultValue?: string;
  onChange?: (event: ChangeEvent<HTMLTextAreaElement>) => void;
  onFocus?: (event: FocusEvent<HTMLTextAreaElement>) => void;
  onBlur?: (event: FocusEvent<HTMLTextAreaElement>) => void;
  rows?: number;
  maxLength?: number;
  name?: string;
  id?: string;
  'aria-label'?: string;
  'aria-invalid'?: boolean;
  className?: string;
}

const sizeStyles: Record<string, string> = {
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-4 py-2 text-base',
  lg: 'px-5 py-2.5 text-lg',
};

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(
  ({
    label,
    error,
    helperText,
    fullWidth = false,
    size = 'md',
    disabled = false,
    placeholder,
    value,
    defaultValue,
    onChange,
    onFocus,
    onBlur,
    rows = 4,
    maxLength,
    name,
    id,
    'aria-label': ariaLabel,
    'aria-invalid': ariaInvalid,
    className = '',
  }: TextareaProps,
  ref,
) => {
  const [characterCount, setCharacterCount] = useState(0);

  const handleChange = (event: ChangeEvent<HTMLTextAreaElement>) => {
    const newValue = event.target.value;
    setCharacterCount(newValue.length);
    onChange?.(event);
  };

  return (
    <div className={`${fullWidth ? 'w-full' : ''}`}>
      {label && (
        <div className="flex items-center justify-between mb-1.5">
          <label className="block text-sm font-medium text-gray-700">{label}</label>
          {maxLength && (
            <span className={`text-sm ${characterCount > maxLength ? 'text-error-600' : 'text-gray-500'}`}>
              {characterCount} / {maxLength}
            </span>
          )}
        </div>
      )}
      <div className="relative">
        <motion.textarea
          ref={ref}
          disabled={disabled}
          placeholder={placeholder}
          value={value}
          defaultValue={defaultValue}
          onChange={handleChange}
          onFocus={onFocus}
          onBlur={onBlur}
          rows={rows}
          maxLength={maxLength}
          name={name}
          id={id}
          aria-label={ariaLabel}
          aria-invalid={ariaInvalid || !!error}
          className={`
            w-full rounded-md border border-gray-300
            focus:outline-none focus:ring-2 focus:ring-primary-500 focus:ring-offset-2
            ${sizeStyles[size]}
            ${error ? 'border-error-500 focus:ring-error-500' : ''}
            ${disabled ? 'opacity-50 cursor-not-allowed bg-gray-50' : 'bg-white'}
            resize-none
            ${className}
          `}
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.2 }}
        />
        {error && <AlertCircle className="absolute right-3 top-3 text-error-500" size={18} />}
      </div>
      {helperText && (
        <p className={`mt-1.5 text-sm ${error ? 'text-error-600' : 'text-gray-500'}`}>
          {helperText}
        </p>
      )}
    </div>
  );
});

Textarea.displayName = 'Textarea';
