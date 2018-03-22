# ORingInspectionAssignment

Uses OpenCV to read in an image of an ORing and detects if there is a fault in the O-Ring.

The assignment find the centre of the O-Ring and checks the edges of the ring to see if the pixels on the edge are a different colour to the Oring object. If the pixels are found to be a different colour the Oring will fail. Uses an erode and fill method to clean Oring.

ORing uses a histogram and threshold method to threshold the image.

The image also uses connecte component labelling to remove any noise, or any other objects which appear in the image outside of the Oring
