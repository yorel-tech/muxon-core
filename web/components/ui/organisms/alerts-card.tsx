'use client';

import { motion, AnimatePresence } from 'framer-motion';
import { XCircle, AlertTriangle, Info, CheckCircle, X } from 'lucide-react';
import { Card, CardContent, CardHeader } from '@/components/ui/atoms/card';
import { Badge } from '@/components/ui/atoms/badge';

export interface Alert {
  id: string;
  severity: 'critical' | 'warning' | 'info' | 'success';
  title: string;
  message: string;
  timestamp: string;
  source?: string;
  action?: {
    label: string;
    onClick: () => void;
  };
  dismissible: boolean;
}

export interface AlertsCardProps {
  alerts: Alert[];
  onDismiss?: (alertId: string) => void;
  onAction?: (alertId: string) => void;
  className?: string;
}

const getSeverityIcon = (severity: Alert['severity']) => {
  switch (severity) {
    case 'critical':
      return <XCircle size={18} className="text-error-500 flex-shrink-0" />;
    case 'warning':
      return <AlertTriangle size={18} className="text-warning-500 flex-shrink-0" />;
    case 'info':
      return <Info size={18} className="text-info-500 flex-shrink-0" />;
    case 'success':
      return <CheckCircle size={18} className="text-success-500 flex-shrink-0" />;
  }
};

const getSeverityBgColor = (severity: Alert['severity']) => {
  switch (severity) {
    case 'critical':
      return 'bg-error-50';
    case 'warning':
      return 'bg-warning-50';
    case 'info':
      return 'bg-info-50';
    case 'success':
      return 'bg-success-50';
  }
};

const getSeverityBorderColor = (severity: Alert['severity']) => {
  switch (severity) {
    case 'critical':
      return 'border-error-200';
    case 'warning':
      return 'border-warning-200';
    case 'info':
      return 'border-info-200';
    case 'success':
      return 'border-success-200';
  }
};

export const AlertsCard = ({
  alerts,
  onDismiss,
  onAction,
  className = '',
}: AlertsCardProps) => {
  const activeAlerts = alerts.filter((alert) => alert.dismissible);
  const criticalCount = alerts.filter((alert) => alert.severity === 'critical').length;
  const warningCount = alerts.filter((alert) => alert.severity === 'warning').length;

  return (
    <Card className={className}>
      <CardHeader>
        <div className="flex items-center justify-between">
          <h2 className="text-xl font-semibold text-gray-900">
            Alerts & Notifications
          </h2>
          <div className="flex items-center gap-2">
            {criticalCount > 0 && (
              <Badge variant="error">{criticalCount} Critical</Badge>
            )}
            {warningCount > 0 && (
              <Badge variant="warning">{warningCount} Warning</Badge>
            )}
            {activeAlerts.length > 0 && criticalCount === 0 && warningCount === 0 && (
              <Badge variant="info">{activeAlerts.length} Active</Badge>
            )}
          </div>
        </div>
      </CardHeader>
      <CardContent className="p-0">
        <div className="p-4 space-y-3 max-h-80 overflow-y-auto">
          <AnimatePresence mode="popLayout">
            {alerts.length === 0 ? (
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="text-center py-8 text-gray-500"
              >
                <Info size={32} className="mx-auto mb-2 text-gray-300" />
                <p>No active alerts</p>
              </motion.div>
            ) : (
              alerts.map((alert) => (
                <motion.div
                  key={alert.id}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, x: -100 }}
                  transition={{ duration: 0.2 }}
                  className={`
                    flex items-start gap-3 p-3 rounded-lg border 
                    ${getSeverityBorderColor(alert.severity)}
                    hover:bg-gray-50 transition-colors
                  `}
                >
                  <div className={`
                    flex-shrink-0 p-2 rounded-full 
                    ${getSeverityBgColor(alert.severity)}
                  `}>
                    {getSeverityIcon(alert.severity)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-sm text-gray-900 truncate">
                          {alert.title}
                        </p>
                        <p className="text-xs text-gray-500 mt-0.5 line-clamp-2">
                          {alert.message}
                        </p>
                        <div className="flex items-center gap-2 mt-1.5">
                          <p className="text-xs text-gray-400">
                            {alert.timestamp}
                          </p>
                          {alert.source && (
                            <>
                              <span className="text-gray-300">•</span>
                              <p className="text-xs text-gray-400 truncate">
                                {alert.source}
                              </p>
                            </>
                          )}
                        </div>
                      </div>
                      <div className="flex items-center gap-1 flex-shrink-0">
                        {alert.action && (
                          <button
                            onClick={() => onAction?.(alert.id)}
                            className="text-xs font-medium text-primary-600 hover:text-primary-700 px-2 py-1 rounded hover:bg-primary-50 transition-colors"
                          >
                            {alert.action.label}
                          </button>
                        )}
                        {alert.dismissible && (
                          <button
                            onClick={() => onDismiss?.(alert.id)}
                            className="p-1 text-gray-400 hover:text-gray-600 rounded hover:bg-gray-100 transition-colors"
                            aria-label="Dismiss alert"
                          >
                            <X size={14} />
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                </motion.div>
              ))
            )}
          </AnimatePresence>
        </div>
      </CardContent>
    </Card>
  );
};
