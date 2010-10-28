# Simple 3D Bird
# by Ira Greenberg
#
# Ruby version
def rsetup
  noStroke
  @ang, @ang2, @ang3, @ang4 = 0, 0, 0, 0
  @px, @py, @pz = 0, 0, 0
  @flapSpeed = 0.2
end

def rdraw
  background(0)
  lights()

  @px = sin(radians(@ang3)) * 170
  @py = cos(radians(@ang3)) * 300
  @pz = sin(radians(@ang4)) * 500
  translate( width / 2 + @px,  height / 2 + @py, -700+@pz)
  rotateX(sin(radians(@ang2)) * 120)
  rotateY(sin(radians(@ang2)) * 50)
  rotateZ(sin(radians(@ang2)) * 65)

  fill(153)
  box(20, 100, 20)

  fill(204)
  pushMatrix()
  rotateY(sin(radians(@ang)) * -20)
  rect(-75, -50, 75, 100)
  popMatrix()

  pushMatrix()
  rotateY(sin(radians(@ang)) * 20)
  rect(0, -50, 75, 100)
  popMatrix()

  @ang += @flapSpeed
  if @ang > 3 or @ang < -3
    @flapSpeed *= -1
  end

  @ang2 += 0.01
  @ang3 += 2.0
  @ang4 += 0.75
end

# helper functions
def sin(x) Math.sin(x) end
def cos(x) Math.cos(x) end
def radians(x) x * (Math::PI / 90) end
