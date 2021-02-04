package cacophonia.ui.graph;

class Vector {
	double angle;
	double force;
	
	public Vector(double force, double angle) {
		this.force = force;
		this.angle = angle;
	}

	public void reset() {
		angle = 0.0;
		force = 0.0;
	}
	
	double getX() {
		return force * Math.cos((Math.PI / 180.0) * angle);
	}

	double getY() {
		return force * Math.sin((Math.PI / 180.0) * angle);
	}

	Vector add(Vector other) {
		double x = other.getX() + getX();
		double y = other.getY() + getY();
		force = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
		angle = Math.toDegrees(Math.atan2(y, x));
		return this;
	}

	@Override
	public String toString() {
		return String.format("Vector[force=%.1f,angle=%.1f]", force, angle);
	}
}