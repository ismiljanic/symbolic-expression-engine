import { motion } from "motion/react";
import { ArrowRight } from "lucide-react";

interface Step {
  label: string;
  description: string;
}

const steps: Step[] = [
  { label: "Input", description: "Matrix parameters" },
  { label: "Symbolic", description: "Algebraic expansion" },
  { label: "Simplify", description: "Closed-form result" },
  { label: "Validate", description: "LU verification" },
];

export function ProcessFlow() {
  return (
    <div className="flex items-center justify-center gap-4 flex-wrap">
      {steps.map((step, index) => (
        <div key={step.label} className="flex items-center gap-4">
          <motion.div
            className="flex flex-col items-center"
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.4, delay: index * 0.15 }}
          >
            <div className="w-16 h-16 bg-gradient-to-br from-blue-50 to-blue-100 rounded-2xl flex items-center justify-center mb-2 border border-blue-100">
              <span className="text-blue-600 font-mono text-sm font-medium">
                {index + 1}
              </span>
            </div>
            <div className="text-center">
              <div className="font-medium text-sm text-gray-900">{step.label}</div>
              <div className="text-xs text-gray-500 mt-0.5">{step.description}</div>
            </div>
          </motion.div>
          {index < steps.length - 1 && (
            <motion.div
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.4, delay: index * 0.15 + 0.2 }}
            >
              <ArrowRight className="w-5 h-5 text-gray-300" />
            </motion.div>
          )}
        </div>
      ))}
    </div>
  );
}
