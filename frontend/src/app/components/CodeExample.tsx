import { motion } from "motion/react";

export function CodeExample() {
  const code = `det(T) = a₁a₂a₃...aₙ - ∑(bᵢcᵢ₊₁) ∏(aⱼ)`;

  return (
    <motion.div
      className="bg-gradient-to-br from-gray-50 to-gray-100 rounded-2xl p-8 border border-gray-200 overflow-hidden"
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.6, delay: 0.4 }}
    >
      <div className="flex items-start gap-4">
        <div className="flex flex-col gap-2">
          <div className="w-2 h-2 rounded-full bg-red-400" />
          <div className="w-2 h-2 rounded-full bg-yellow-400" />
          <div className="w-2 h-2 rounded-full bg-green-400" />
        </div>
        <div className="flex-1">
          <div className="text-xs text-gray-500 mb-3 font-mono">Symbolic Expression</div>
          <div className="font-mono text-sm text-gray-800 leading-relaxed">
            {code}
          </div>
          <div className="mt-4 pt-4 border-t border-gray-200">
            <div className="text-xs text-gray-500 mb-2">Simplified Polynomial Form</div>
            <div className="font-mono text-xs text-gray-600">
              Result: <span className="text-blue-600">O(n) complexity</span>
            </div>
          </div>
        </div>
      </div>
    </motion.div>
  );
}
