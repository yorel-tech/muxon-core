'use client';

import { motion, AnimatePresence } from 'framer-motion';
import { forwardRef, useEffect } from 'react';
import { CheckCircle, X, AlertCircle, Info, AlertTriangle, XCircle } from 'lucide-react';

export interface ToastProps {
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  message: string;
  variant?: 'success' | 'error' | 'warning' | 'info';
  duration?: number;
}

const variantIcons: Record<string, React.ReactNode> = {
  success: <CheckCircle className="text-success-500" />,
  error: <XCircle className="text-error-500" />,
  warning: <AlertTriangle className="text-warning-500" />,
  info: <Info className="text-info-500" />,
};

const variantStyles: Record<string, string> = {
  success: 'border-l-4 border-success-500 bg-success-50 text-success-800',
  error: 'border-l-4 border-error-500 bg-error-50 text-error-800',
  warning: 'border-l-4 border-warning-500 bg-warning-50 text-warning-800',
  info: 'border-l-4 border-info-500 bg-info-50 text-info-800',
};

export const Toast = forwardRef<HTMLDivElement, ToastProps>(
  ({ isOpen, onClose, title, message, variant = 'success', duration = 5000 }, ref) => {
    useEffect(() => {
      if (isOpen && duration > 0) {
        const timer = setTimeout(onClose, duration);
        return () => clearTimeout(timer);
      };
    }, [isOpen, onClose, duration]);

    return (
      <AnimatePresence>
        {isOpen && (
          <motion.div
            ref={ref}
            className="fixed bottom-4 right-4 z-50"
            initial={{ opacity: 0, y: 50 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: 50 }}
            transition={{ duration: 0.3 }}
          >
            <div className={`
              flex items-start gap-3 p-4 rounded-lg shadow-lg
              ${variantStyles[variant]}
              min-w-[300px] max-w-md
            `}>
              <div className="flex items-start gap-3">
                {variantIcons[variant]}
                <div className="flex-shrink-0 mt-0.5">
                  {variant === 'success' && <CheckCircle className="text-success-500" />}
                  {variant === 'error' && <XCircle className="text-error-500" />}
                  {variant === 'warning' && <AlertTriangle className="text-warning-500" />}
                  {variant === 'info' && <Info className="text-info-500" />}
                </div>
                <div className="flex-1">
                  {title && (
                    <h3 className="text-sm font-semibold text-gray-900">{title}</h3>
                  )}
                  <p className="text-sm text-gray-600">{message}</p>
                </div>
                <button
                  onClick={onClose}
                  className="text-gray-400 hover:text-gray-600 transition-colors"
                  aria-label="Close toast"
                >
                  <X size={16} />
                </button>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    );
  }
);

Toast.displayName = 'Toast';
