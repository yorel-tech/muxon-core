'use client';

import { motion, AnimatePresence } from 'framer-motion';
import { forwardRef, useState, useRef, useEffect, type ReactNode } from 'react';
import { ChevronDown, Check } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface DropdownOption {
  value: string;
  label: string;
  disabled?: boolean;
  icon?: ReactNode;
  description?: string;
  onClick?: () => void;
}

export interface DropdownProps {
  options: DropdownOption[];
  value?: string;
  onChange?: (value: string) => void;
  placeholder?: string;
  label?: string;
  error?: string;
  helperText?: string;
  disabled?: boolean;
  leftIcon?: ReactNode;
  rightIcon?: ReactNode;
  className?: string;
  position?: 'left' | 'right';
  align?: 'start' | 'center' | 'end';
}

export const Dropdown = forwardRef<HTMLDivElement, DropdownProps>(
  ({
    options,
    value,
    onChange,
    placeholder = 'Select...',
    label,
    error,
    helperText,
    disabled = false,
    leftIcon,
    rightIcon = <ChevronDown size={16} />,
    className = '',
    position = 'left',
    align = 'start',
  }: DropdownProps,
  ref,
) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  const selectedOption = options.find((opt) => opt.value === value);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleSelect = (optionValue: string) => {
    onChange?.(optionValue);
    setIsOpen(false);
  };

  return (
    <div ref={ref} className={cn('relative', className)}>
      {label && (
        <label className="block text-sm font-medium text-gray-700 mb-1.5">
          {label}
        </label>
      )}
      <div className="relative">
        <button
          onClick={() => !disabled && setIsOpen(!isOpen)}
          disabled={disabled}
          className={cn(
            'w-full flex items-center justify-between px-3 py-2.5 border border-gray-300 rounded-md text-left text-sm transition-colors',
            {
              'bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent': true,
              'bg-gray-50 cursor-not-allowed': disabled,
              'border-error-300 focus:ring-error-500': error,
            },
          )}
        >
          <div className="flex items-center gap-2 flex-1">
            {leftIcon && <span className="text-gray-400">{leftIcon}</span>}
            <span
              className={cn(
                'flex-1 truncate',
                !value && 'text-gray-500',
              )}
            >
              {selectedOption ? selectedOption.label : placeholder}
            </span>
          </div>
          <motion.span
            animate={{ rotate: isOpen ? 180 : 0 }}
            transition={{ duration: 0.2 }}
            className="text-gray-400"
          >
            {rightIcon}
          </motion.span>
        </button>

        <AnimatePresence>
          {isOpen && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              transition={{ duration: 0.2 }}
              className={cn(
                'absolute z-50 mt-1 w-full bg-white border border-gray-200 rounded-lg shadow-lg py-1 max-h-60 overflow-y-auto',
                {
                  'left-0': position === 'left',
                  'right-0': position === 'right',
                },
              )}
            >
              {options.map((option) => (
                <button
                                  key={option.value}
                                  onClick={() => {
                                    !option.disabled && handleSelect(option.value);
                                    option.onClick?.();
                                  }}
                                  disabled={option.disabled}
                                  className={cn(
                                    'w-full px-3 py-2 text-left text-sm transition-colors flex items-start gap-3',
                                    {
                                      'hover:bg-gray-100': !option.disabled,
                                      'cursor-pointer': !option.disabled,
                                      'cursor-not-allowed opacity-50': option.disabled,
                                      'bg-gray-50': option.value === value,
                                    },
                                  )}
                                >
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      {option.icon && (
                        <span className="text-gray-400">{option.icon}</span>
                      )}
                      <span className="font-medium text-gray-900">{option.label}</span>
                    </div>
                    {option.description && (
                      <p className="text-xs text-gray-500 mt-0.5">
                        {option.description}
                      </p>
                    )}
                  </div>
                  {option.value === value && (
                    <Check size={16} className="text-primary-600" />
                  )}
                </button>
              ))}
            </motion.div>
          )}
        </AnimatePresence>
      </div>
      {helperText && (
        <p className="mt-1.5 text-xs text-gray-500">{helperText}</p>
      )}
      {error && (
        <p className="mt-1.5 text-xs text-error-600">{error}</p>
      )}
    </div>
  );
});

Dropdown.displayName = 'Dropdown';
