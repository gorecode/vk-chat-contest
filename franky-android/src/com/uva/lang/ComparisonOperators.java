package com.uva.lang;

public final class ComparisonOperators {
	public static final class Default implements ComparisonOperator {
		public boolean equals(Object a, Object b) {
			if (a == b) return true;
			if ((a == null) || (b == null)) return false;
			return a.equals(b);
		}
	};

	public static final class Inverse implements ComparisonOperator {
		private final ComparisonOperator mOp;

		public Inverse(ComparisonOperator op) {
			mOp = op;
		}

		public boolean equals(Object a, Object b) {
			return !mOp.equals(a, b);
		}
	}

	private ComparisonOperators() {
		;
	}
}
