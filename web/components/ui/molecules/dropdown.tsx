'use client';

import { motion, AnimatePresence } from 'framer-motion';
import { forwardRef, useState, useEffect, useRef, type ReactNode } from 'react';
import { createPortal } from 'react-dom';
import { ChevronDown, Check } from 'lucide-react';
import { cn } from '@/lib/utils';

export interface DropdownOption {
  label: string;
  value?: string;
  variant?: 'default' | 'danger' | 'success' | 'warning';
  disabled?: boolean;
  icon?: React.ReactNode;
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
  trigger?: ReactNode;
  children?: ReactNode;
  usePortal?: boolean; // New prop to enable portal rendering
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
    trigger,
    children,
    usePortal = false, // Default to false for backward compatibility
  },
    ref,
  ) => {
  const [isOpen, setIsOpen] = useState(false);
  const [menuPosition, setMenuPosition] = useState({ top: 0, left: 0, width: 0 });
  const dropdownRef = useRef<HTMLDivElement>(null);
  const triggerRef = useRef<HTMLDivElement>(null);

  const selectedOption = options.find((opt) => opt.value === value || opt.label === value);

  // Update menu position when opening
  useEffect(() => {
    if (isOpen && usePortal && triggerRef.current) {
      const rect = triggerRef.current.getBoundingClientRect();
      setMenuPosition({
        top: rect.bottom + window.scrollY,
        left: position === 'right' ? rect.right + window.scrollX - 200 : rect.left + window.scrollX,
        width: rect.width,
      });
    }
  }, [isOpen, position, usePortal]);

  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (usePortal) {
        // When using portal, check if click is outside both dropdown and trigger
        if (
          dropdownRef.current &&
          triggerRef.current &&
          !dropdownRef.current.contains(event.target as Node) &&
          !triggerRef.current.contains(event.target as Node)
        ) {
          setIsOpen(false);
        }
      } else {
        // Original behavior for non-portal mode
        if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
          setIsOpen(false);
        }
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [usePortal]);

  const handleSelect = (optionValue: string, option: DropdownOption) => {
    onChange?.(option.value || optionValue);
    setIsOpen(false);
  };

  const TriggerComponent = trigger || children;

  // Render the dropdown menu
  const renderDropdownMenu = () => (
    <AnimatePresence>
      {isOpen && (
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -10 }}
          transition={{ duration: 0.2 }}
          className={cn(
            'bg-white border border-gray-200 rounded-lg shadow-lg py-1 z-[9999]',
            usePortal ? 'fixed' : 'absolute mt-1 w-full',
            {
              'left-0': !usePortal && position === 'left',
              'right-0': !usePortal && position === 'right',
            },
          )}
          style={usePortal ? {
            top: `${menuPosition.top}px`,
            left: `${menuPosition.left}px`,
            width: `${menuPosition.width}px`,
          } : {}}
        >
          {options.map((option) => (
            <button
              key={option.label}
              onClick={() => {
                !option.disabled && handleSelect(option.label, option);
                option.onClick?.();
              }}
              disabled={option.disabled}
              className={cn(
                'w-full px-3 py-2 text-left text-sm transition-colors flex items-start gap-3 whitespace-normal',
                {
                  'hover:bg-gray-100': !option.disabled,
                  'cursor-pointer': !option.disabled,
                  'cursor-not-allowed opacity-50': option.disabled,
                  'bg-gray-50': (option.value || option.label) === value,
                },
              )}
            >
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
              {option.label === value && (
                <Check size={16} className="text-primary-600" />
              )}
            </button>
          ))}
        </motion.div>
      )}
    </AnimatePresence>
  );

  return (
    <div ref={ref} className={cn('relative', className)}>
      {label && (
        <label className="block text-sm font-medium text-gray-700 mb-1.5">
          {label}
        </label>
      )}
      <div className="relative" ref={triggerRef}>
        {TriggerComponent ? (
          <div onClick={(e) => { e.stopPropagation(); !disabled && setIsOpen(!isOpen); }}>
            {TriggerComponent}
          </div>
        ) : (
          <button
            onClick={(e) => { e.stopPropagation(); !disabled && setIsOpen(!isOpen); }}
            disabled={disabled}
            className={cn(
              'w-full flex items-center justify-between px-3 py-2.5 border border-gray-300 rounded-md text-left text-sm transition-colors',
              {
                'bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent': true,
                'bg-gray-50 cursor-not-allowed': disabled,
                'border-error-300 focus:ring-error-500': error,
              },
            )}
          >
            <span className="flex-1 truncate">
              {selectedOption ? selectedOption.label : placeholder}
            </span>
            <span className="text-gray-400">
              {rightIcon}
            </span>
          </button>
        )}
        {!usePortal && renderDropdownMenu()}
      </div>
      {usePortal && isOpen && createPortal(
        <div ref={dropdownRef}>
          {renderDropdownMenu()}
        </div>,
        document.body
      )}
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
