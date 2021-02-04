package cacophonia.ui.graph;

import org.junit.jupiter.api.Test;

import junit.framework.TestCase;


class VectorTest extends TestCase {
	Graph graph = new Graph(new Settings());
	
	@Test
	void test_x_0() {
		check_X(100.0, 0, 100.0);
	}
	
	@Test
	void test_x_45() {
		check_X(100.0, 45, 70.71);
	}
	
	@Test
	void test_x_90() {
		check_X(100.0, 90, 0.0);
	}
	
	@Test
	void test_x_135() {
		check_X(100.0, 135, -70.71);
	}
	
	@Test
	void test_x_180() {
		check_X(100.0, 180, -100.0);
	}
	
	@Test
	void test_x_225() {
		check_X(100.0, 225, -70.71);
	}
	
	@Test
	void test_x_270() {
		check_X(100.0, 270.0, 0.0);
	}
	
	@Test
	void test_x_315() {
		check_X(100.0, 315, 70.71);
	}
	
	@Test
	void test_x_360() {
		check_X(100, 360, 100.0);
	}
	
	void check_X(double force, double angle, double x) {
		assertEquals(x, new Vector(force, angle).getX(), 0.01);
	}
	
	@Test
	void test_y_0() {
		check_Y(100.0, 0, 0.0);
	}
	
	@Test
	void test_y_45() {
		check_Y(100.0, 45, 70.71);
	}
	
	@Test
	void test_y_90() {
		check_Y(100.0, 90, 100.0);
	}
	
	@Test
	void test_y_135() {
		check_Y(100.0, 135, 70.71);
	}
	
	@Test
	void test_y_180() {
		check_Y(100.0, 180, 0.0);
	}
	
	@Test
	void test_y_225() {
		check_Y(100.0, 225, -70.71);
	}
	
	@Test
	void test_y_270() {
		check_Y(100.0, 270.0, -100.0);
	}
	
	@Test
	void test_y_315() {
		check_Y(100.0, 315, -70.71);
	}
	
	@Test
	void test_y_360() {
		check_Y(100, 360, 0.0);
	}
	
	void check_Y(double force, double angle, double y) {
		assertEquals(y, new Vector(force, angle).getY(), 0.01);
	}
	
	@Test
	void test_add_to_45_degree_x() {
		assertEquals(100.0, add_vectors(100.0, 90, 100.0, 0).getX(), 0.01);
	}
	
	@Test
	void test_add_to_90_degree_x() {
		Vector vector = add_vectors(100.0, 45, 100.0, 135);
		double x = vector.getX();
		assertEquals(0.0, x, 0.01);
	}

	@Test
	void test_add_to_180_degree_x() {
		assertEquals(0.0, add_vectors(100.0, 180, 100.0, 0).getX(), 0.01);
	}
	
	@Test
	void test_add_to_45_degree_y() {
		assertEquals(100.0, add_vectors(100.0, 90, 100.0, 0).getY(), 0.01);
	}
	
	@Test
	void test_add_to_90_degree_y() {
		assertEquals(141.42, add_vectors(100.0, 45, 100.0, 135).getY(), 0.01);
	}

	@Test
	void test_add_to_180_degree_y() {
		assertEquals(0.0, add_vectors(100.0, 180, 100.0, 0).getY(), 0.01);
	}

	Vector add_vectors(double force1, double angle1, double force2, double angle2) {
		Vector vector1 = new Vector(force1, angle1);
		Vector vector2 = new Vector(force2, angle2);
		return vector1.add(vector2);
	}
	
}
	